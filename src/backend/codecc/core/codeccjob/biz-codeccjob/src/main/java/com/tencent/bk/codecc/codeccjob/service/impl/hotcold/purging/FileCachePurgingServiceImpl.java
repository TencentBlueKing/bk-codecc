package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.purging;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_DATA_SEPARATION;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DATA_SEPARATION_FILE_CACHE_PURGING;

import com.tencent.devops.common.constant.ComConstants.ColdDataPurgingType;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 文件缓存清理
 */
@Service
public class FileCachePurgingServiceImpl extends AbstractDefectPurgingTemplate {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    protected long purgeCore(long taskId) {
        // 保留已修复的所有版本 && 待修复的最后一个版本
        rabbitTemplate.convertAndSend(
                EXCHANGE_DATA_SEPARATION,
                ROUTE_DATA_SEPARATION_FILE_CACHE_PURGING,
                taskId,
                message -> {
                    message.getMessageProperties().setDelayLong(TimeUnit.SECONDS.toMillis(5));
                    return message;
                }
        );

        return 0L;
    }

    @Override
    public ColdDataPurgingType coldDataPurgingType() {
        return ColdDataPurgingType.FILE_CACHE;
    }
}
