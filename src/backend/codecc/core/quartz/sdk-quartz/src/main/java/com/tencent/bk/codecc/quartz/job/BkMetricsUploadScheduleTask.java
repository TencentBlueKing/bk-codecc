package com.tencent.bk.codecc.quartz.job;

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 蓝盾度量上报CodeCC
 *
 * @date 2022/6/20
 */

public class BkMetricsUploadScheduleTask implements IScheduleTask {

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Override
    public void executeTask(@NotNull QuartzJobContext quartzJobContext) {
        rabbitTemplate.convertAndSend("exchange.metrics.statistic.trigger", "route.metrics.statistic.trigger", "");
    }
}
