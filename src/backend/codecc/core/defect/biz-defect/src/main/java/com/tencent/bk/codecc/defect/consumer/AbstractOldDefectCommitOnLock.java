package com.tencent.bk.codecc.defect.consumer;

import com.tencent.bk.codecc.defect.component.ScmJsonComponent;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.web.mq.ConstantsKt;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

@Slf4j
@Deprecated
public abstract class AbstractOldDefectCommitOnLock {

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;
    @Autowired
    private ScmJsonComponent scmJsonComponent;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 上锁超时时间，30分钟
    private static final int COMMIT_LOCK_EXPIRE_TIME = 30 * 60;

    /**
     * 业务recommit
     *
     * @param originVO 原始VO
     * @param createFrom
     */
    protected void recommit(CommitDefectVO originVO, String createFrom) {
        // 工蜂不加锁，无需recommit
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(createFrom)) {
            return;
        }

        String streamName = originVO.getStreamName();
        String toolName = originVO.getToolName();
        String buildId = originVO.getBuildId();

        long fileSize = scmJsonComponent.getDefectFileSize(streamName, toolName, buildId);
        String toolPattern = toolMetaCacheService.getToolPattern(toolName).toLowerCase(Locale.ENGLISH);
        String exchange;
        String routingKey;

        if (fileSize > 1024 * 1024 * 1024) {
            log.warn("告警文件大小超过1G: {}", fileSize);
            exchange = ConstantsKt.EXCHANGE_DEFECT_COMMIT_SUPER_LARGE;
            routingKey = ConstantsKt.ROUTE_DEFECT_COMMIT_SUPER_LARGE;
        } else if (fileSize > 1024 * 1024 * 200 && fileSize < 1024 * 1024 * 1024) {
            log.warn("告警文件大于200M小于1G: {}", fileSize);
            exchange = String.format("%s%s.large", ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT, toolPattern);
            routingKey = String.format("%s%s.large", ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT, toolPattern);
        } else {
            log.info("告警文件小于200M: {}", fileSize);
            exchange = String.format("%s%s.new", ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT, toolPattern);
            routingKey = String.format("%s%s.new", ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT, toolPattern);
        }

        originVO.setRecommitTimes(originVO.getRecommitTimes() == null ? 1 : originVO.getRecommitTimes() + 1);

        // 若快速调配到队尾3次，还没能成功消费，则降级为延迟消息10s，防止边界业务过渡消耗；每3次会触发1次延迟消息
        if (originVO.getRecommitTimes() % 3 == 0) {
            rabbitTemplate.convertAndSend(exchange, routingKey, originVO, delayMQProcessor(10));
        } else {
            rabbitTemplate.convertAndSend(exchange, routingKey, originVO);
        }
    }

    /**
     * 延迟消息处理器
     *
     * @param delayInSecond
     * @return
     */
    protected MessagePostProcessor delayMQProcessor(Integer delayInSecond) {
        return message -> {
            message.getMessageProperties().setDelay(delayInSecond * 1000);
            return message;
        };
    }

    /**
     * 获取redis分布式锁
     *
     * @param taskId
     * @param toolName
     * @return
     */
    protected RedisLock getLock(Long taskId, String toolName) {
        String key = String.format("COMMIT_DEFECT_LOCK:%d:%s", taskId, toolName);

        return new RedisLock(redisTemplate, key, COMMIT_LOCK_EXPIRE_TIME);
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
}
