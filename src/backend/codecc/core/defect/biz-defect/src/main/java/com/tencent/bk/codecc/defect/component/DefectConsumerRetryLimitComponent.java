package com.tencent.bk.codecc.defect.component;

import com.tencent.bk.codecc.defect.dao.mongorepository.common.DefectConsumerRetryLimitRepository;
import com.tencent.bk.codecc.defect.model.common.DefectConsumerRetryLimitLog;
import com.tencent.devops.common.constant.ComConstants.DefectConsumerType;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 限制消息重复消费次数的工具类
 */
@Slf4j
@Component
public class DefectConsumerRetryLimitComponent {

    /**
     * 默认的重复限制次数 5 次
     */
    private static final Integer DEFAULT_RETRY_LIMIT = 5;
    /**
     * 重复消费次数记录REDIS 前缀
     */
    private static final String REDIS_KEY_PREFIX = "CONSUMER_RETRY:";
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private DefectConsumerRetryLimitRepository defectConsumerRetryLimitRepository;

    /**
     * 检查是否达到了限制，如果没有
     *
     * @param taskId
     * @param buildId
     * @param toolName
     * @param type
     * @param message
     * @return
     */
    public boolean checkIfReachRetryLimit(Long taskId, String buildId, String toolName, DefectConsumerType type,
            String message) {
        // 组装KEY值
        String key = String.format(REDIS_KEY_PREFIX + "%d:%s:%s:%s", taskId, buildId, toolName, type.name());
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count.intValue() > DEFAULT_RETRY_LIMIT) {
            // 达到限制，记录
            log.error("checkIfReachRetryLimit taskId:{}. build:{}. toolName:{}. type:{}. reach limit:{}",
                    taskId, buildId, toolName, type.name(), DEFAULT_RETRY_LIMIT);
            logReachRetryLimitMessage(taskId, buildId, toolName, type, message);
            return true;
        }
        log.info("checkIfReachRetryLimit taskId:{}. build:{}. toolName:{}. type:{}. retry count: {}",
                taskId, buildId, toolName, type.name(), count);
        // 未达限制，续期
        stringRedisTemplate.expire(key, 1, TimeUnit.HOURS);
        return false;
    }

    private void logReachRetryLimitMessage(Long taskId, String buildId, String toolName, DefectConsumerType type,
            String message) {
        DefectConsumerRetryLimitLog log = new DefectConsumerRetryLimitLog(taskId, buildId, toolName, type.name(),
                message);
        long curTime = System.currentTimeMillis();
        log.setCreatedDate(curTime);
        log.setUpdatedDate(curTime);
        defectConsumerRetryLimitRepository.save(log);
    }

}
