package com.tencent.bk.codecc.defect.consumer;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.web.mq.ConstantsKt;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.util.StringUtils;

/**
 * 提单锁模式抽象，防止提单过程并发引入脏乱数据
 * 注：工蜂来源的任务不上锁
 */
@Slf4j
public abstract class AbstractDefectCommitOnLock {

    @Autowired
    protected RabbitTemplate rabbitTemplate;
    @Autowired
    protected RedisTemplate<String, String> redisTemplate;

    // 上锁超时时间，30分钟
    private static final int COMMIT_LOCK_EXPIRE_TIME = 30 * 60;

    // 不区分告警文件大小的工具
    private static final Set<String> RECOMMIT_TOOLS_NOT_BY_FILE_SIZE
            = Sets.newHashSet(ToolPattern.COVERITY.name(), ToolPattern.KLOCWORK.name());

    /**
     * 提单过程在锁模式下允许继续执行吗
     * 注：不允许执行的话会进行recommit操作，无需外部重复处理
     *
     * @param vo mq消息体
     * @return true继续，false应中断业务执行
     */
    protected Pair<Boolean, RedisLock> continueWithLock(CommitDefectVO vo) {
        String createFrom = vo.getCreateFrom();
        long taskId = vo.getTaskId();
        String toolName = vo.getToolName();
        String buildId = vo.getBuildId();
        Long fileSize = vo.getDefectFileSize();

        // 开源扫描不加锁
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(createFrom)) {
            throw new UnsupportedOperationException("gongfeng scan no need lock");
        }

        boolean mustByFileSize = !RECOMMIT_TOOLS_NOT_BY_FILE_SIZE.contains(toolName.toUpperCase(Locale.ENGLISH));
        if (mustByFileSize && fileSize == null) {
            throw new IllegalArgumentException("file size arg must not be null");
        }

        RedisLock locker = getLocker(taskId, toolName);

        try {
            boolean lockSuccess = locker.tryLock();
            log.info("defect commit, try lock {}, {}, {}, {}", taskId, toolName, buildId, lockSuccess);

            if (!lockSuccess) {
                logRecommitTimes(vo);

                if (mustByFileSize) {
                    recommitByFileSize(vo, fileSize);
                } else {
                    recommit(vo);
                }

                log.info("defect commit, recommit {}, {}, {}", taskId, toolName, buildId);

                return Pair.of(false, locker);
            }

            return Pair.of(true, locker);
        } catch (Throwable t) {
            log.error("continueWithLock error, {}, {}, {}", taskId, buildId, toolName, t);
            // 锁的释放在调用方，防止return前出现异常导致一直等待锁
            if (locker.isLocked()) {
                locker.unlock();
            }

            throw t;
        }
    }

    /**
     * 提单过程锁模式关闭，应急开关
     *
     * @return true为开关打开，业务不再加锁
     */
    protected boolean lockModeIsClosed() {
        String flag = redisTemplate.opsForValue().get("COMMIT_DEFECT_LOCK_IS_CLOSED");

        return !StringUtils.isEmpty(flag) && "1".equals(flag);
    }

    /**
     * 延迟消息处理器
     *
     * @param delayInSecond
     * @return
     */
    protected MessagePostProcessor delayMQProcessor(Integer delayInSecond) {
        return message -> {
            message.getMessageProperties().setDelayLong((long) delayInSecond * 1000);
            return message;
        };
    }

    protected void logRecommitTimes(CommitDefectVO vo) {
        if (vo == null) {
            return;
        }

        vo.setRecommitTimes(vo.getRecommitTimes() == null ? 1 : vo.getRecommitTimes() + 1);
    }

    protected abstract String getRecommitMQExchange(CommitDefectVO vo);

    protected abstract String getRecommitMQRoutingKey(CommitDefectVO vo);

    /**
     * 除coverity、klocwork之外，其余工具都需按文件大小决策投递MQ
     *
     * @param vo
     * @param fileSize
     */
    private void recommitByFileSize(CommitDefectVO vo, long fileSize) {
        String exchange;
        String routingKey;

        if (fileSize > 1024 * 1024 * 1024) {
            log.warn("告警文件大小超过1G: {}", fileSize);
            exchange = ConstantsKt.EXCHANGE_DEFECT_COMMIT_SUPER_LARGE;
            routingKey = ConstantsKt.ROUTE_DEFECT_COMMIT_SUPER_LARGE;
        } else if (fileSize > 1024 * 1024 * 200 && fileSize < 1024 * 1024 * 1024) {
            log.warn("告警文件大于200M小于1G: {}", fileSize);
            exchange = String.format("%s.large", getRecommitMQExchange(vo));
            routingKey = String.format("%s.large", getRecommitMQRoutingKey(vo));
        } else {
            log.info("告警文件小于200M: {}", fileSize);
            exchange = String.format("%s.new", getRecommitMQExchange(vo));
            routingKey = String.format("%s.new", getRecommitMQRoutingKey(vo));
        }

        publishToMQ(exchange, routingKey, vo);
    }

    /**
     * coverity、klocwork无需按告警文件大小分级
     *
     * @param vo
     */
    private void recommit(CommitDefectVO vo) {
        publishToMQ(getRecommitMQExchange(vo), getRecommitMQRoutingKey(vo), vo);
    }

    private void publishToMQ(String exchange, String routingKey, CommitDefectVO vo) {
        // 若快速调配到队尾3次，还没能成功消费，则降级为延迟消息10s，防止边界业务过渡消耗；每3次会触发1次延迟消息
        if (vo.getRecommitTimes() % 3 == 0) {
            rabbitTemplate.convertAndSend(exchange, routingKey, vo, delayMQProcessor(10));
        } else {
            rabbitTemplate.convertAndSend(exchange, routingKey, vo);
        }
    }

    private RedisLock getLocker(Long taskId, String toolName) {
        String key = String.format("COMMIT_DEFECT_LOCK:%d:%s", taskId, toolName);

        return new RedisLock(redisTemplate, key, COMMIT_LOCK_EXPIRE_TIME);
    }
}
