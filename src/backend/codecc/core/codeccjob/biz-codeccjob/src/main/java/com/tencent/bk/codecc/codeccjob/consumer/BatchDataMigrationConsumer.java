package com.tencent.bk.codecc.codeccjob.consumer;

import static com.tencent.devops.common.constant.ComConstants.DATA_MIGRATION_VIRTUAL_BUILD_ID;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_DEFECT_MIGRATION_TRIGGER_BATCH;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_DEFECT_MIGRATION_TRIGGER_BATCH;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DEFECT_MIGRATION_TRIGGER_BATCH;

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CommonDefectMigrationRepository;
import com.tencent.bk.codecc.defect.model.CommonDefectMigrationEntity;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.web.mq.ConstantsKt;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 批量数据迁移，该Consumer更多是承担发起者的角色
 */
@Component
@Slf4j
public class BatchDataMigrationConsumer {

    private static final String REDIS_KEY_LAST_TASK_ID = "BATCH_DATA_MIGRATION:COMMON_TO_LINT:TASK_ID";
    private static final String REDIS_LOCK_KEY = "LOCK_KEY:BATCH_DATA_MIGRATION:COMMON_TO_LINT";
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private Client client;
    @Autowired
    private CommonDefectMigrationRepository commonDefectMigrationRepository;


    /**
     * 批量触发数据迁移
     *
     * @param object
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    key = ROUTE_DEFECT_MIGRATION_TRIGGER_BATCH,
                    value = @Queue(value = QUEUE_DEFECT_MIGRATION_TRIGGER_BATCH, durable = "true"),
                    exchange = @Exchange(
                            value = EXCHANGE_DEFECT_MIGRATION_TRIGGER_BATCH,
                            durable = "true", delayed = "true", type = "topic"
                    )
            ),
            concurrency = "1"
    )
    public void batchCommonToLint(Object object) {
        RedisLock lock = new RedisLock(redisTemplate, REDIS_LOCK_KEY, TimeUnit.HOURS.toSeconds(2));
        try {
            // 集群只消费1次，锁期间的后来者当重复直接丢弃
            if (!lock.tryLock()) {
                log.info("batch common to lint data migration, get lock fail, drop this mq message");
                return;
            }

            log.info("batch common to lint data migration begin");
            batchCommonToLintCore();
            log.info("batch common to lint data migration end");
        } catch (Throwable t) {
            log.error("batch common to lint data migration error", t);
        } finally {
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
    }

    private void batchCommonToLintCore() {
        long beginTime = System.currentTimeMillis();
        int loopCounter = 0;
        String lastTaskIdStr = redisTemplate.opsForValue().get(REDIS_KEY_LAST_TASK_ID);
        long lastTaskId = StringUtils.isEmpty(lastTaskIdStr) ? 0L : Long.parseLong(lastTaskIdStr);
        List<TaskBaseVO> taskList = client.get(ServiceTaskRestResource.class)
                .getTaskInfoForDataMigration(lastTaskId, 1000)
                .getData();

        while (!CollectionUtils.isEmpty(taskList)) {
            // 每下发一批都校验下开关，防止突发情况
            if (!isOnBatchMigrationMode()) {
                log.info("batch common to lint data migration, switch is closed");
                break;
            }

            // 部分任务在日常构建中，可能已经迁移过了；需进一步过滤筛选
            Map<Long, String> map = filterByMigrationDone(taskList);

            for (Entry<Long, String> kv : map.entrySet()) {
                long taskId = kv.getKey();
                String createFrom = kv.getValue();
                CommitDefectVO mqMsg = new CommitDefectVO();
                mqMsg.setTaskId(taskId);
                mqMsg.setStreamName("virtual-stream-name");
                // 任意一款common类工具均可触发任务维度的迁移
                mqMsg.setToolName(ToolPattern.COVERITY.name());
                mqMsg.setBuildId(DATA_MIGRATION_VIRTUAL_BUILD_ID);
                mqMsg.setTriggerFrom("codeccjob");
                mqMsg.setMessage("");
                mqMsg.setCreateFrom(createFrom);
                mqMsg.setDefectFileSize(0L);

                if (BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(createFrom)) {
                    rabbitTemplate.convertAndSend(
                            ConstantsKt.EXCHANGE_DEFECT_MIGRATION_COMMON_OPENSOURCE,
                            ConstantsKt.ROUTE_DEFECT_MIGRATION_COMMON_OPENSOURCE,
                            mqMsg
                    );
                } else {
                    rabbitTemplate.convertAndSend(
                            ConstantsKt.EXCHANGE_DEFECT_MIGRATION_COMMON,
                            ConstantsKt.ROUTE_DEFECT_MIGRATION_COMMON,
                            mqMsg
                    );
                }
            }

            // 记录上次执行到的位置，以支持增量
            long endTaskId = taskList.get(taskList.size() - 1).getTaskId();
            long beginTaskId = taskList.get(0).getTaskId();
            redisTemplate.opsForValue().set(REDIS_KEY_LAST_TASK_ID, String.valueOf(endTaskId));

            log.info("batch common to lint data migration, list size: {}, actual size: {}, "
                            + "loop: {}, cost: {}, begin task id: {}, end task id: {}",
                    taskList.size(), map.size(), ++loopCounter, System.currentTimeMillis() - beginTime,
                    beginTaskId, endTaskId);

            // 每10秒下发一批，每批1000个task
            sleep(10);

            beginTime = System.currentTimeMillis();
            taskList = client.get(ServiceTaskRestResource.class)
                    .getTaskInfoForDataMigration(endTaskId, 1000)
                    .getData();
        }
    }

    /**
     * 批量迁移开关
     *
     * @return true为开启批量，false为关闭批量
     */
    private boolean isOnBatchMigrationMode() {
        return "1".equals(redisTemplate.opsForValue().get(ComConstants.DATA_MIGRATION_SWITCH_BATCH_MODE));
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(1000L * seconds);
            // NOCC:EmptyCatchBlock(设计如此:)
        } catch (InterruptedException ignore) {

        }
    }

    /**
     * 把已执行过的任务过滤掉
     *
     * @param taskBaseVOList
     * @return key: taskId, value: createFrom
     */
    private Map<Long, String> filterByMigrationDone(List<TaskBaseVO> taskBaseVOList) {
        Map<Long, String> retMap = taskBaseVOList.stream()
                .filter(x -> x.getTaskId() > 0 && !StringUtils.isEmpty(x.getCreateFrom()))
                .collect(Collectors.toMap(TaskBaseVO::getTaskId, TaskBaseVO::getCreateFrom, (k1, k2) -> k2));

        Set<Long> migrationDoneTaskIds = commonDefectMigrationRepository.findByTaskIdIn(retMap.keySet())
                .stream()
                .map(CommonDefectMigrationEntity::getTaskId)
                .collect(Collectors.toSet());

        retMap.entrySet().removeIf(x -> migrationDoneTaskIds.contains(x.getKey()));

        return retMap;
    }

}
