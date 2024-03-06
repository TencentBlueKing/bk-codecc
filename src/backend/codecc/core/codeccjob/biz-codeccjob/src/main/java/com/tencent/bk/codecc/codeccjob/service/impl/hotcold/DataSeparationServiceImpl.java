package com.tencent.bk.codecc.codeccjob.service.impl.hotcold;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_DATA_SEPARATION;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DATA_SEPARATION_COOL_DOWN;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.ColdDataPurgingLogRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.OperationHistoryDao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.TaskLogOverviewDao;
import com.tencent.bk.codecc.codeccjob.service.ColdDataArchivingService;
import com.tencent.bk.codecc.codeccjob.service.ColdDataPurgingService;
import com.tencent.bk.codecc.codeccjob.service.ColdDataWarmingService;
import com.tencent.bk.codecc.codeccjob.service.DataSeparationService;
import com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo.HotColdConstants;
import com.tencent.bk.codecc.defect.model.ColdDataPurgingLogEntity;
import com.tencent.bk.codecc.defect.model.OperationHistoryEntity;
import com.tencent.bk.codecc.defect.model.TaskLogOverviewEntity;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.GetLatestBuildIdMapRequest;
import com.tencent.bk.codecc.task.vo.GetTaskStatusAndCreateFromResponse;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import com.tencent.devops.common.constant.ComConstants.ColdDataPurgingType;
import com.tencent.devops.common.constant.ComConstants.TaskStatus;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.util.ThreadUtils;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * 冷热分离核心业务类
 */
@Slf4j
@Service
public class DataSeparationServiceImpl implements ApplicationContextAware, DataSeparationService {

    private Collection<ColdDataArchivingService> coldDataArchivingServices;
    private Collection<ColdDataPurgingService> sortedColdDataPurgingServices;
    private Collection<ColdDataWarmingService> coldDataWarmingServices;
    @Autowired
    private Client client;
    @Autowired
    private TaskLogOverviewDao taskLogOverviewDao;
    @Autowired
    private OperationHistoryDao operationHistoryDao;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private ColdDataPurgingLogRepository coldDataPurgingLogRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        coldDataArchivingServices = applicationContext.getBeansOfType(ColdDataArchivingService.class).values();
        coldDataWarmingServices = applicationContext.getBeansOfType(ColdDataWarmingService.class).values();
        sortedColdDataPurgingServices = applicationContext.getBeansOfType(ColdDataPurgingService.class).values()
                .stream()
                .sorted(Comparator.comparing(ColdDataPurgingService::order))
                .collect(Collectors.toList());
    }

    /**
     * 降冷任务下发
     */
    @Override
    public void coolDownTrigger() {
        long lessThanEqualTime = System.currentTimeMillis() - HotColdConstants.TO_COLD_MILLIS_DIFF;
        long lastTaskId = 0L;
        int batchSize = 1000;

        while (true) {
            List<Long> taskIdList = client.get(ServiceTaskRestResource.class)
                    .getTaskIdListForHotColdDataSeparation(lastTaskId, batchSize)
                    .getData();
            if (CollectionUtils.isEmpty(taskIdList)) {
                break;
            }

            Set<Long> finalTaskIdSet = filterByOpLogTime(
                    filterByLatestBuildTime(taskIdList, lessThanEqualTime),
                    lessThanEqualTime
            );
            if (!CollectionUtils.isEmpty(finalTaskIdSet)) {
                for (Long taskId : finalTaskIdSet) {
                    rabbitTemplate.convertAndSend(EXCHANGE_DATA_SEPARATION, ROUTE_DATA_SEPARATION_COOL_DOWN, taskId);
                }

                log.info("cool down trigger batch task id: {}", finalTaskIdSet);
            }

            if (taskIdList.size() < batchSize) {
                break;
            }

            lastTaskId = taskIdList.get(taskIdList.size() - 1);
            // 每10秒下发一批
            ThreadUtils.sleep(TimeUnit.SECONDS.toMillis(10));
        }
    }

    /**
     * 数据降冷
     *
     * @param taskId
     */
    @Override
    public void coolDown(long taskId) {
        List<RedisLock> lockers = getLockers(taskId);

        try {
            boolean lockSuccess = tryLock(lockers, taskId);
            // 上锁失败意味着该任务正在提单，可能已经重新激活了，直接丢弃
            if (!lockSuccess) {
                log.info("cool down get lock fail, task id: {}", taskId);
                return;
            }

            // double-check
            GetTaskStatusAndCreateFromResponse taskResp =
                    client.get(ServiceTaskRestResource.class).getTaskStatusAndCreateFrom(taskId).getData();
            if (taskResp == null
                    || taskResp.getStatus() == TaskStatus.COLD.value()
                    || BsTaskCreateFrom.GONGFENG_SCAN.value().equals(taskResp.getCreateFrom())) {
                log.info("cool down not match, task id: {}", taskId);
                return;
            }

            // double-check
            long lessThanEqualTime = System.currentTimeMillis() - HotColdConstants.TO_COLD_MILLIS_DIFF;
            Set<Long> finalTaskIdSet = filterByOpLogTime(
                    filterByLatestBuildTime(Lists.newArrayList(taskId), lessThanEqualTime),
                    lessThanEqualTime
            );
            if (CollectionUtils.isEmpty(finalTaskIdSet)) {
                log.info("cool down not match, task id: {}", taskId);
                return;
            }

            boolean archiveSuccess = archiveColdData(taskId);
            if (archiveSuccess) {
                boolean purgeSuccess = purgeColdData(taskId);
                if (purgeSuccess) {
                    client.get(ServiceTaskRestResource.class).setTaskToColdFlag(taskId);
                    log.info("cool down success, task id: {}", taskId);
                }
            }
        } finally {
            unlock(lockers);
        }
    }

    /**
     * 数据加热
     *
     * @param taskId
     */
    @Override
    public void warmUp(long taskId) {
        List<RedisLock> lockers = getLockers(taskId);
        try {
            boolean lockSuccess = tryLock(lockers, taskId);
            // 提单那边是按(任务+工具)维度处理的，加热逻辑是按任务维度，所以任务是有可能重复多次的，直接丢弃
            if (!lockSuccess) {
                log.info("warm up get lock fail, task id: {}", taskId);
                return;
            }

            GetTaskStatusAndCreateFromResponse taskResp =
                    client.get(ServiceTaskRestResource.class).getTaskStatusAndCreateFrom(taskId).getData();
            if (taskResp == null || TaskStatus.COLD.value() != taskResp.getStatus()
                    || BsTaskCreateFrom.GONGFENG_SCAN.value().equals(taskResp.getCreateFrom())) {
                log.info("warm up not match, task id: {}", taskId);
                return;
            }

            boolean isSuccess = true;
            for (ColdDataWarmingService coldDataWarmingService : coldDataWarmingServices) {
                try {
                    coldDataWarmingService.warm(taskId);
                } catch (Throwable t) {
                    log.error("warm cold data fail, task id: {}, type: {}", taskId,
                            coldDataWarmingService.coldDataArchivingType().name(), t);

                    isSuccess = false;
                    break;
                }
            }

            if (isSuccess) {
                client.get(ServiceTaskRestResource.class).setTaskToEnableFlag(taskId);
                log.info("warm up success, task id: {}", taskId);
            }
        } finally {
            unlock(lockers);
        }
    }

    @Override
    public void upsertPurgingLog(long taskId, long delCount, long cost, boolean finalResult) {
        ColdDataPurgingLogEntity entity = coldDataPurgingLogRepository.findFirstByTaskIdAndType(
                taskId,
                ColdDataPurgingType.FILE_CACHE.name()
        );

        if (entity == null) {
            entity = new ColdDataPurgingLogEntity(
                    taskId,
                    ColdDataPurgingType.FILE_CACHE.name(),
                    finalResult,
                    delCount,
                    "",
                    cost
            );
            entity.applyAuditInfoOnCreate();
        } else {
            entity.setDataCount(delCount);
            entity.setSuccess(finalResult);
            entity.setCost(cost);
            entity.applyAuditInfoOnUpdate();
        }

        coldDataPurgingLogRepository.save(entity);
    }

    private List<RedisLock> getLockers(long taskId) {
        List<String> toolNameList = client.get(ServiceTaskRestResource.class).getTaskToolNameList(taskId).getData();
        if (CollectionUtils.isEmpty(toolNameList)) {
            return Lists.newArrayList();
        }

        List<RedisLock> redisLockList = Lists.newArrayList();
        for (String toolName : toolNameList) {
            // 跟提单的锁一致
            String key = String.format("COMMIT_DEFECT_LOCK:%d:%s", taskId, toolName);
            redisLockList.add(new RedisLock(redisTemplate, key, HotColdConstants.TO_COLD_COMMIT_LOCK_EXPIRE_TIME));
        }

        return redisLockList;
    }

    private boolean tryLock(List<RedisLock> lockers, long taskId) {
        if (CollectionUtils.isEmpty(lockers)) {
            return false;
        }

        // 这锁是冷热分层消费者之间互斥
        RedisLock tempLock = new RedisLock(
                redisTemplate,
                String.format("HOT_COLD_DATA_SEPARATION:%d", taskId),
                TimeUnit.SECONDS.toSeconds(10)
        );

        try {
            if (!tempLock.tryLock()) {
                return false;
            }

            // 这锁是跟提单消费者之间互斥
            for (RedisLock locker : lockers) {
                boolean b = locker.tryLock();
                if (!b) {
                    return false;
                }
            }
        } finally {
            if (tempLock.isLocked()) {
                tempLock.unlock();
            }
        }

        return true;
    }

    private void unlock(List<RedisLock> lockers) {
        if (CollectionUtils.isEmpty(lockers)) {
            return;
        }

        for (RedisLock redisLock : lockers) {
            try {
                if (redisLock.isLocked()) {
                    redisLock.unlock();
                }
                // NOCC:EmptyCatchBlock(设计如此:)
            } catch (Throwable ignore) {
            }
        }
    }

    private boolean archiveColdData(long taskId) {
        for (ColdDataArchivingService archivingService : coldDataArchivingServices) {
            boolean b = archivingService.archive(taskId);
            // 遇错即停
            if (!b) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean purgeColdData(long taskId) {
        for (ColdDataPurgingService purgingService : sortedColdDataPurgingServices) {
            boolean b = purgingService.purge(taskId);
            // 遇错即停
            if (!b) {
                return false;
            }
        }

        return true;
    }

    private Set<Long> filterByLatestBuildTime(List<Long> taskIdList, long timePoint) {
        GetLatestBuildIdMapRequest request = new GetLatestBuildIdMapRequest(taskIdList);
        Map<Long, String> taskIdToBuildIdMap = client.get(ServiceTaskRestResource.class)
                .latestBuildIdMap(request)
                .getData();

        if (taskIdToBuildIdMap == null || taskIdToBuildIdMap.size() == 0) {
            return Sets.newHashSet();
        }

        // buildId为空的，均列入清理名单，属历史脏数据
        Set<Long> finalTaskIdSet = taskIdToBuildIdMap.entrySet().stream()
                .filter(x -> ObjectUtils.isEmpty(x.getValue()))
                .map(Entry::getKey)
                .collect(Collectors.toSet());

        // buildId不为空的，以overview为依据校验最后构建时间
        Map<Long, String> taskIdToBuildIdNotEmptyMap = taskIdToBuildIdMap.entrySet().stream()
                .filter(x -> !ObjectUtils.isEmpty(x.getValue()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        finalTaskIdSet.addAll(
                taskLogOverviewDao.findByTaskIdAndBuildIdAndStartTimeLessThanEqual(
                                taskIdToBuildIdNotEmptyMap,
                                timePoint
                        ).stream()
                        .map(TaskLogOverviewEntity::getTaskId)
                        .collect(Collectors.toList())
        );

        return finalTaskIdSet;
    }

    private Set<Long> filterByOpLogTime(Set<Long> taskIdSet, long timePoint) {
        if (CollectionUtils.isEmpty(taskIdSet)) {
            return Sets.newHashSet();
        }

        // 大于timePoint还存在操作日志的任务Id
        Set<Long> opTaskIdSet = operationHistoryDao.findByTaskIdInAndTimeGreaterThan(taskIdSet, timePoint).stream()
                .map(OperationHistoryEntity::getTaskId)
                .collect(Collectors.toSet());

        taskIdSet.removeAll(opTaskIdSet);

        return taskIdSet;
    }
}
