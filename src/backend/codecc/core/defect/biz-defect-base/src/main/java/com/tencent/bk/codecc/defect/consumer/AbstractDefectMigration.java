package com.tencent.bk.codecc.defect.consumer;

import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.devops.common.redis.lock.RedisLock;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.util.StringUtils;

/**
 * 告警数据迁移抽象
 */
@Slf4j
public abstract class AbstractDefectMigration extends AbstractDefectCommitOnLock {

    /**
     * 完成数据迁移后，继续执行正常业务逻辑吗
     *
     * @param vo
     * @return true则代表完成迁移，可回归到正常业务处理；false意味着竞争锁失败，业务方无需额外处理，自动recommit
     */
    protected boolean continueWithDataMigration(CommitDefectVO vo) {
        long taskId = vo.getTaskId();
        String buildId = vo.getBuildId();
        // 已经迁移过，无论结果如何，不再重复执行
        if (hasMigrated(taskId)) {
            return true;
        }

        // 按整体任务维度上锁一次性迁移
        RedisLock locker = getLocker_5Minutes(taskId);
        long beginTime = System.currentTimeMillis();

        try {
            boolean lockSuccess = locker.tryLock();
            log.info("defect migration, try lock, task id: {}, build id: {}, lock: {}", taskId, buildId, lockSuccess);

            if (!lockSuccess) {
                recommitForDataMigration(vo);
                log.info("defect migration, lock fail to recommit, task id: {}, build id: {}", taskId, buildId);

                return false;
            }

            // double-check
            if (hasMigrated(taskId)) {
                log.info("defect migration, double check, already do migration, task id: {}, build id: {}", taskId,
                        buildId);
                return true;
            }

            // 触发人
            String triggerUser = StringUtils.isEmpty(vo.getTriggerFrom()) ? "system" : vo.getTriggerFrom();

            for (String toolName : matchToolList()) {
                long innerBeginTime = System.currentTimeMillis();
                doMigration(taskId, toolName, triggerUser);
                log.info("defect migration, loop by tool, task id: {}, build id: {}, tool name: {}, cost total: {}",
                        taskId, buildId, toolName, System.currentTimeMillis() - innerBeginTime);
            }

            return true;
        } finally {
            if (locker.isLocked()) {
                locker.unlock();
            }

            log.info("defect migration, task id: {}, build id: {}, cost total: {}",
                    taskId, buildId, System.currentTimeMillis() - beginTime);
        }
    }

    protected void recommitForDataMigration(CommitDefectVO vo) {
        logRecommitTimes(vo);
        String exchange = getRecommitMQExchange(vo);
        String routingKey = getRecommitMQRoutingKey(vo);

        // 前3次延迟30秒，大于3次则提高至60秒
        MessagePostProcessor messagePostProcessor = vo.getRecommitTimes() > 3
                ? delayMQProcessor(60)
                : delayMQProcessor(30);

        rabbitTemplate.convertAndSend(exchange, routingKey, vo, messagePostProcessor);
    }

    /**
     * 是否执行过迁移
     */
    protected abstract boolean hasMigrated(long taskId);


    /**
     * 数据迁移核心逻辑
     */
    protected abstract void doMigration(long taskId, String toolName, String triggerUser);

    /**
     * 数据迁移涉及到的关联工具
     */
    protected abstract Collection<String> matchToolList();


    private RedisLock getLocker_5Minutes(long taskId) {
        String key = String.format("COMMON_DEFECT_MIGRATION:%d", taskId);

        return new RedisLock(redisTemplate, key, 60 * 5);
    }
}
