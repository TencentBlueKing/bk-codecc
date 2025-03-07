package com.tencent.bk.codecc.codeccjob.config

import com.tencent.bk.codecc.codeccjob.consumer.IgnoreApprovalTimingCheckConsumer
import com.tencent.codecc.common.mq.AbstractMQConfig
import com.tencent.devops.common.web.mq.EXCHANGE_CODECC_IGNORE_APPROVAL_TIMING_CHECK
import com.tencent.devops.common.web.mq.QUEUE_CODECC_IGNORE_APPROVAL_TIMING_CHECK
import com.tencent.devops.common.web.mq.ROUTE_CODECC_IGNORE_APPROVAL_TIMING_CHECK
import org.springframework.amqp.core.CustomExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class IgnoreApprovalTimingCheckRabbitMQConfig : AbstractMQConfig(
    exchangeName = EXCHANGE_CODECC_IGNORE_APPROVAL_TIMING_CHECK,
    queueName = QUEUE_CODECC_IGNORE_APPROVAL_TIMING_CHECK,
    routeName = ROUTE_CODECC_IGNORE_APPROVAL_TIMING_CHECK
) {

    @Bean
    fun ignoreApprovalTimingCheckExchange() = abstractExchange()

    @Bean
    fun ignoreApprovalTimingCheckQueue() = abstractQueue()

    @Bean
    fun ignoreApprovalTimingCheckBinding(
        @Autowired ignoreApprovalTimingCheckExchange: CustomExchange,
        @Autowired ignoreApprovalTimingCheckQueue: Queue
    ) = abstractBinding(ignoreApprovalTimingCheckQueue, ignoreApprovalTimingCheckExchange)

    @Bean
    fun ignoreApprovalTimingCheckListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired ignoreApprovalTimingCheckConsumer: IgnoreApprovalTimingCheckConsumer
    ) = abstractListenerContainer(
        ignoreApprovalTimingCheckConsumer,
        ignoreApprovalTimingCheckConsumer::consumer.name,
        connectionFactory
    )
}
