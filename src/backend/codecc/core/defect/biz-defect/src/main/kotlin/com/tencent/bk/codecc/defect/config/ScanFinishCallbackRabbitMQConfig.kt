package com.tencent.bk.codecc.defect.config

import com.tencent.bk.codecc.defect.condition.DefectCondition
import com.tencent.bk.codecc.defect.consumer.ScanFinishCallbackConsumer
import com.tencent.codecc.common.mq.AbstractMQConfig
import com.tencent.devops.common.web.mq.EXCHANGE_SCAN_FINISH
import com.tencent.devops.common.web.mq.QUEUE_SCAN_FINISH_FOR_CALLBACK
import com.tencent.devops.common.web.mq.ROUTE_SCAN_FINISH
import org.springframework.amqp.core.CustomExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional

import org.springframework.context.annotation.Configuration

@Configuration
@Conditional(DefectCondition::class)
class ScanFinishCallbackRabbitMQConfig : AbstractMQConfig(
    exchangeName = EXCHANGE_SCAN_FINISH,
    queueName = QUEUE_SCAN_FINISH_FOR_CALLBACK,
    routeName = ROUTE_SCAN_FINISH
) {
    @Bean
    fun scanFinishCallbackExchange() = abstractExchange()

    @Bean
    fun scanFinishCallbackQueue() = abstractQueue()

    @Bean
    fun scanFinishCallbackBinding(
        @Autowired scanFinishCallbackExchange: CustomExchange,
        @Autowired scanFinishCallbackQueue: Queue
    ) = abstractBinding(scanFinishCallbackQueue, scanFinishCallbackExchange)

    @Bean
    fun scanFinishCallbackListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired scanFinishCallbackConsumer: ScanFinishCallbackConsumer
    ) = abstractListenerContainer(
        scanFinishCallbackConsumer,
        scanFinishCallbackConsumer::consumer.name,
        connectionFactory
    )
}
