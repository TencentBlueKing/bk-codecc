package com.tencent.bk.codecc.codeccjob.consumer;


import com.tencent.bk.codecc.codeccjob.service.DataSeparationService;
import com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo.HotColdConstants;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.service.IConsumer;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataSeparationCoolDownTriggerConsumer implements IConsumer<Long> {

    @Autowired
    private DataSeparationService dataSeparationService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void consumer(Long timestamp) {
        RedisLock lock = new RedisLock(
                redisTemplate,
                HotColdConstants.DATA_SEPARATION_TRIGGER_LOCK_KEY,
                TimeUnit.HOURS.toSeconds(12)
        );

        try {
            // 集群只消费1次，锁期间的后来者当重复直接丢弃
            if (!lock.tryLock()) {
                log.info("DataSeparationCoolDownTriggerConsumer get lock fail");
                return;
            }

            log.info("DataSeparationCoolDownTriggerConsumer begin, param: {}", timestamp);
            dataSeparationService.coolDownTrigger();
            log.info("DataSeparationCoolDownTriggerConsumer end, param: {}", timestamp);
        } catch (Throwable t) {
            log.error("DataSeparationCoolDownTriggerConsumer fail, param: {}", timestamp, t);
        } finally {
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
    }
}
