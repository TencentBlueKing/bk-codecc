package com.tencent.bk.codecc.scanschedule.consumer;

import com.tencent.bk.codecc.scanschedule.pojo.record.ScanRecord;
import com.tencent.bk.codecc.scanschedule.service.ToolScanService;
import com.tencent.devops.common.web.aop.annotation.EndReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 触发工具扫描消息队列的消费者
 *
 * @version V1.0
 * @date 2019/10/17
 */
@Component
@Slf4j
public class ToolScanConsumer {

    @Autowired
    private ToolScanService toolScanService;

    /**
     * 触发工具扫描
     *
     * @param scanRecord
     */
    @RabbitListener(
            bindings = @QueueBinding(key = ROUTE_SCANSCHEDULE_TOOL_SCAN,
                    value = @Queue(value = QUEUE_SCANSCHEDULE_TOOL_SCAN, durable = "true"),
                    exchange = @Exchange(
                            value = EXCHANGE_SCANSCHEDULE_TOOL_SCAN,
                            durable = "true",
                            delayed = "true",
                            type = "topic"
                    )
            )
    )
    public ScanRecord toolScan(ScanRecord scanRecord) {
        return toolScanService.scan(scanRecord);
    }
}
