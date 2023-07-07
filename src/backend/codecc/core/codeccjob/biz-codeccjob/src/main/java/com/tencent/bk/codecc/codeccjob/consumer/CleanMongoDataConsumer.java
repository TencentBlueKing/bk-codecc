package com.tencent.bk.codecc.codeccjob.consumer;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.CleanMongoDataFailTaskRepository;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.CleanMongoDataLogRepository;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.TaskLogOverviewRepository;
import com.tencent.bk.codecc.codeccjob.service.ICleanMongoDataService;
import com.tencent.bk.codecc.defect.model.CleanMongoDataLogEntity;
import com.tencent.bk.codecc.defect.model.CleanMongoFailTaskEntity;
import com.tencent.bk.codecc.defect.model.TaskLogOverviewEntity;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.ServiceToolConfigRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.IPUtils;
import com.tencent.devops.common.util.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.joda.time.LocalDateTime;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom.API_TRIGGER;
import static com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom.TIMING_SCAN;
import static com.tencent.devops.common.constant.ComConstants.CLEAN_TASK_WHITE_LIST;

/**
 * 定时清理 defect 服务 mongo 数据，涉及：
 * t_task_log
 * t_task_log_overview
 * t_dupc_statistic
 * t_ccn_statistic
 * t_lint_statistic
 * t_cloc_statistic
 * t_stat_statistic
 * t_statistic
 * t_tool_build_stack
 * t_build_defect
 * t_build_defect_v2
 * t_build_defect_summary
 * t_build
 * t_build_defect_snapshott
 * t_metrics
 *
 * 按照 build_id 为组清理
 */
@Slf4j
@Component
public class CleanMongoDataConsumer implements ApplicationContextAware {

    @Autowired
    private Client client;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private TaskLogOverviewRepository taskLogOverviewRepository;

    @Autowired
    private CleanMongoDataLogRepository cleanMongoDataLogRepository;

    @Autowired
    private CleanMongoDataFailTaskRepository cleanMongoDataFailTaskRepository;

    private Map<String, ICleanMongoDataService> cleanServiceMap;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        cleanServiceMap = applicationContext.getBeansOfType(ICleanMongoDataService.class);
    }

    /**
     * 清理数据调度逻辑，根据 defect 节点数和当前 taskId 最大值计算分页，
     * 拿到每个 taskId 的废弃 buildId 集合，
     * 会根据 buildId 清理对应快找记录，到 6 点终止清理任务
     */
    public void consumer(String param) {
        try {
            Result<List<BaseDataVO>> result = client.get(ServiceBaseDataResource.class)
                    .getParamsByType(ComConstants.CLEAN_NODE_THREAD_NUM);

            int parallelNum = 2;
            if (result.getData() != null && !result.getData().isEmpty()) {
                parallelNum = Integer.parseInt(result.getData().get(0).getParamValue());
            }

            List<Future<CleanMongoDataLogEntity>> futureList = new ArrayList<>(parallelNum);
            for (int i = 0; i < parallelNum; i++) {
                int threadId = i;
                int finalParallelNum = parallelNum;
                futureList.add(ThreadPoolUtil.addCallableTask(
                        () -> clean(threadId, finalParallelNum)
                ));
            }

            for (Future<CleanMongoDataLogEntity> future : futureList) {
                future.get();
            }
        } catch (Throwable e) {
            log.error("fail to start clean thread: {}", ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * 清理任务主线逻辑
     * @param threadId 当前线程ID
     * @param parallelNum 每个节点并发数
     */
    private CleanMongoDataLogEntity clean(int threadId, int parallelNum) {
        // 初始化清理日志实体类
        CleanMongoDataLogEntity cleanMongoDataLogEntity = initCleanLog(threadId);
        long startTime = System.currentTimeMillis();
        int cleanedTaskSize = 0;
        try {
            // 获取当前节点线程要清理的 task 列表
            List<TaskDetailVO> taskDetailVOList = getTaskList(cleanMongoDataLogEntity, parallelNum);
            log.info("begin to clean mongo data: clean node: {} {} {} {}",
                    cleanMongoDataLogEntity.getNodeIp(), threadId, taskDetailVOList.size(), System.currentTimeMillis());

            // 获取任务结束时间，默认 6 点结束
            Result<List<BaseDataVO>> endTimeResult = client.get(ServiceBaseDataResource.class)
                    .getParamsByType(ComConstants.CLEAN_CONSUMER_END_TIME);
            int endTime = 6;
            if (endTimeResult.getData() != null && !endTimeResult.getData().isEmpty()) {
                endTime = Integer.parseInt(endTimeResult.getData().get(0).getParamValue());
            }

            // 获取快照保留条数，默认保留 5000 次构建
            Result<List<BaseDataVO>> indexResult = client.get(ServiceBaseDataResource.class)
                    .getParamsByType(ComConstants.MAX_SAVE_BUILD_NUM);
            Map<String, Integer> indexMap = new HashMap<>();

            if (indexResult.isNotOk() || indexResult.getData() == null || indexResult.getData().isEmpty()) {
                log.error("clean mongo data fail: can not get MAX_SAVE_BUILD_NUM");
                indexMap.put("index", 5000);
                indexMap.put(API_TRIGGER.value(), 5000);
                indexMap.put(TIMING_SCAN.value(), 14);
            } else {
                indexMap.put("index", Integer.parseInt(indexResult.getData().get(0).getParamValue()));
                indexMap.put(API_TRIGGER.value(), Integer.parseInt(indexResult.getData().get(0).getParamExtend1()));
                indexMap.put(TIMING_SCAN.value(), Integer.parseInt(indexResult.getData().get(0).getParamExtend2()));
            }

            for (TaskDetailVO taskDetailVO : taskDetailVOList) {
                try {
                    // 每天早上六点终止此定时任务，没清理完第二天继续
                    int now = LocalDateTime.now().getHourOfDay();
                    long taskId = taskDetailVO.getTaskId();
                    String projectId = taskDetailVO.getProjectId();
                    if (StringUtils.isBlank(projectId)) {
                        continue;
                    }
                    if (now >= endTime) {
                        log.info("clean mongo data time out: clean taskId: {}", taskDetailVO);
                        break;
                    }
                    List<String> obsoleteBuildIdList = getObsoleteBuildIdList(taskDetailVO, indexMap);
                    if (!obsoleteBuildIdList.isEmpty()) {
                        List<String> taskToolList = getTaskTools(taskId);
                        clean(projectId, taskId, obsoleteBuildIdList, taskToolList);
                    }

                    cleanedTaskSize++;
                } catch (Throwable e) {
                    // 记录清理失败任务记录
                    String exceptionStackTrace = ExceptionUtils.getStackTrace(e);
                    String exceptionMessage = ExceptionUtils.getMessage(e);
                    log.error("can not clean mongo data: {} {}",
                            taskDetailVO, exceptionStackTrace);
                    recordFailTask(taskDetailVO.getTaskId(),
                            exceptionStackTrace,
                            exceptionMessage,
                            cleanMongoDataLogEntity.getCleanDate(),
                            cleanMongoDataLogEntity.getNodeIp());
                }
            }

            log.info("end clean mongo data: {}", System.currentTimeMillis());
        } catch (Throwable e) {
            cleanMongoDataLogEntity.setStackTrace(ExceptionUtils.getStackTrace(e));
            cleanMongoDataLogEntity.setMessage(ExceptionUtils.getMessage(e));
        } finally {
            long endTime = System.currentTimeMillis();
            cleanMongoDataLogEntity.setCostTime(endTime - startTime);
            cleanMongoDataLogEntity.setCleanedTaskSize(cleanedTaskSize);
            cleanMongoDataLogRepository.save(cleanMongoDataLogEntity);
        }
        return cleanMongoDataLogEntity;
    }

    /**
     * 分段清理历史数据
     * @param taskId
     * @param obsoleteBuildIdList
     * @param taskToolList
     */
    private void clean(String projectId, long taskId, List<String> obsoleteBuildIdList, List<String> taskToolList) {
        log.info("begin to clean data by build id: {} {} {}", taskId, obsoleteBuildIdList.size(), taskToolList.size());
        // 构建号按 500 个一组分批删除
        List<List<String>> partitionList = Lists.partition(obsoleteBuildIdList, 500);
        partitionList.forEach(it -> cleanServiceMap
                .values()
                .forEach(service -> service.clean(projectId, taskId, it, taskToolList)));
    }

    /**
     * 初始化清理日志记录，记录当前节点IP、开始时间
     * @param threadId
     */
    private CleanMongoDataLogEntity initCleanLog(int threadId) {
        log.info("init clean log: {}", threadId);
        Integer cleanDate = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        String nodeIp = IPUtils.INSTANCE.getInnerIP();
        CleanMongoDataLogEntity cleanMongoDataLogEntity = new CleanMongoDataLogEntity();
        cleanMongoDataLogEntity.setCleanDate(cleanDate);
        cleanMongoDataLogEntity.setNodeIp(nodeIp);
        cleanMongoDataLogEntity.setThreadId(threadId);
        cleanMongoDataLogEntity = cleanMongoDataLogRepository.save(cleanMongoDataLogEntity);
        return cleanMongoDataLogEntity;
    }

    /**
     * 获取指定 taskId 的废弃 buildId 集合
     * 根据 buildNum 排序取得；
     * 注：taskLogOverview 表数据不全，所以拿到的 buildId 集合也不全，有部分老数据无法在这里获取到
     *
     * @param taskDetailVO
     * @param indexMap
     * @return 废弃 buildId 集合
     */
    protected List<String> getObsoleteBuildIdList(TaskDetailVO taskDetailVO, Map<String, Integer> indexMap) {
        List<TaskLogOverviewEntity> taskLogOverviewList
                = taskLogOverviewRepository.findByTaskId(taskDetailVO.getTaskId());

        int index = indexMap.getOrDefault(taskDetailVO.getTaskType(), indexMap.get("index"));
        if (CLEAN_TASK_WHITE_LIST.equals(taskDetailVO.getTaskType()) && taskDetailVO.getCleanIndex() != null) {
            index = taskDetailVO.getCleanIndex();
        }
        if (taskLogOverviewList != null && !taskLogOverviewList.isEmpty()) {
            return taskLogOverviewList.stream()
                    .sorted(Comparator.comparingInt(i -> {
                        // 没有构建号的是旧数据，放在排序末尾直接删除
                        if (StringUtils.isBlank(i.getBuildNum())) {
                            return -1;
                        }
                        return -(Integer.parseInt(i.getBuildNum()));
                    }))
                    .map(TaskLogOverviewEntity::getBuildId)
                    .skip(index)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    /***
     * 拿到当前任务的工具信息，包括以前使用过，现在已经下架的工具
     *
     * @param taskId
     */
    public List<String> getTaskTools(long taskId) {
        Result<List<ToolConfigInfoVO>> result = client.get(ServiceToolConfigRestResource.class).getTaskIdByPage(taskId);
        if (result.isNotOk() || result.getData() == null) {
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR, "get tool config fail");
        }

        return result.getData()
                .stream()
                .map(ToolConfigInfoVO::getToolName)
                .collect(Collectors.toList());
    }

    /**
     * 根据当前最大 taskId 和 defect 节点的数量，
     * 计算每个defect节点应该处理多少个task的数据，
     * 并返回当前节点应处理哪一段 taskId 区间。
     *
     * @return index: 当前应该处理第几段 taskId； totalTask： 每一段有多少个 task
     */
    private List<TaskDetailVO> getTaskList(CleanMongoDataLogEntity cleanMongoDataLogEntity, int parallelNum) {
        // 拿最大 taskid，然后拿分段，清理制定分段的 task 信息
        long nodeNum = client.getServiceNodeNum("codeccjob");
        Result<Long> taskSizeResult = client.get(ServiceTaskRestResource.class).countTaskSize();
        if (taskSizeResult.isNotOk() || taskSizeResult.getData() == null || taskSizeResult.getData() == 0L) {
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR, "get task size fail");
        }

        long partition = nodeNum * parallelNum;
        long totalSize = taskSizeResult.getData();
        long currSize = totalSize / partition;
        if (currSize * partition < totalSize) {
            currSize += 1;
        }
        long index = redisTemplate.opsForValue().increment(RedisKeyConstants.CLEAN_DATA_TASK_LIST, 1);
        log.info("clean data node index: {}, totalSize: {}, currSize: {}, nodeNum: {}, partition: {}",
                index, totalSize, currSize, nodeNum, partition);
        Result<List<TaskDetailVO>> taskIsListResult = client.get(ServiceTaskRestResource.class)
                .getTaskIdByPage((int) index - 1, (int) currSize);
        if (taskIsListResult.isNotOk() || taskIsListResult.getData() == null) {
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR, "get task ids fail");
        }

        List<TaskDetailVO> taskDetailVOList = taskIsListResult.getData();
        if (index >= partition) {
            redisTemplate.opsForValue().set(RedisKeyConstants.CLEAN_DATA_TASK_LIST, "0");
        }

        recordCleanDetail(cleanMongoDataLogEntity, taskDetailVOList, (int) index, nodeNum, partition);
        return taskDetailVOList;
    }

    /**
     * 记录清理数据当前节点信息
     * @param cleanMongoDataLogEntity
     * @param taskDetailVOList
     * @param index
     * @param nodeNum
     */
    private void recordCleanDetail(CleanMongoDataLogEntity cleanMongoDataLogEntity,
                                   List<TaskDetailVO> taskDetailVOList,
                                   int index,
                                   long nodeNum,
                                   long partition) {
        cleanMongoDataLogEntity.setNodeIndex(index);
        cleanMongoDataLogEntity.setNodeNum(nodeNum);
        cleanMongoDataLogEntity.setPartition(partition);
        cleanMongoDataLogEntity.setCleanTaskSize(taskDetailVOList.size());
    }

    /**
     * 记录清理失败的任务信息、失败原因、节点信息等
     * @param taskId
     * @param stackTrace
     * @param message
     * @param cleanDate
     * @param nodeIp
     */
    private void recordFailTask(long taskId, String stackTrace, String message, Integer cleanDate, String nodeIp) {
        CleanMongoFailTaskEntity cleanMongoFailTaskEntity = new CleanMongoFailTaskEntity();
        cleanMongoFailTaskEntity.setTaskId(taskId);
        cleanMongoFailTaskEntity.setCleanDate(cleanDate);
        cleanMongoFailTaskEntity.setStackTrace(stackTrace);
        cleanMongoFailTaskEntity.setMessage(message);
        cleanMongoFailTaskEntity.setNodeIp(nodeIp);
        cleanMongoDataFailTaskRepository.save(cleanMongoFailTaskEntity);
    }
}
