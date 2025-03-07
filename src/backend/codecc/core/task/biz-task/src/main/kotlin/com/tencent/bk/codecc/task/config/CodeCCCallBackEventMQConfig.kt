package com.tencent.bk.codecc.task.config

import com.tencent.bk.codecc.task.consumer.CodeCCCallBackEventConsumer
import com.tencent.codecc.common.mq.AbstractMQConfig
import com.tencent.devops.common.web.mq.EXCHANGE_CODECC_CALLBACK_EVENT
import com.tencent.devops.common.web.mq.QUEUE_CODECC_CALLBACK_EVENT
import com.tencent.devops.common.web.mq.ROUTE_CODECC_CALLBACK_EVENT
import org.springframework.amqp.core.CustomExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CodeCCCallBackEventMQConfig : AbstractMQConfig(
    exchangeName = EXCHANGE_CODECC_CALLBACK_EVENT,
    queueName = QUEUE_CODECC_CALLBACK_EVENT,
    routeName = ROUTE_CODECC_CALLBACK_EVENT
) {

    @Bean
    fun codeccCallbackEventExchange() = abstractExchange()

    @Bean
    fun codeccCallbackEventQueue() = abstractQueue()

    @Bean
    fun codeccCallbackEventBinding(
        @Autowired codeccCallbackEventExchange: CustomExchange,
        @Autowired codeccCallbackEventQueue: Queue
    ) = abstractBinding(codeccCallbackEventQueue, codeccCallbackEventExchange)

    @Bean
    fun codeccCallbackEventQueueListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired codeCCCallBackEventConsumer: CodeCCCallBackEventConsumer
    ) = abstractListenerContainer(
        codeCCCallBackEventConsumer,
        codeCCCallBackEventConsumer::consumer.name,
        connectionFactory
    )
}
