package com.tencent.bk.codecc.codeccjob.config

import com.tencent.bk.codecc.codeccjob.consumer.LLMFilterProgressMQConsumer
import com.tencent.codecc.common.mq.AbstractMQConfig
import com.tencent.devops.common.service.utils.IPUtils
import com.tencent.devops.common.web.mq.EXCHANGE_LLM_FILTER_PROGRESS
import com.tencent.devops.common.web.mq.QUEUE_LLM_FILTER_PROGRESS
import com.tencent.devops.common.web.mq.ROUTE_LLM_FILTER_PROGRESS
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 大模型误报过滤进度 MQ
 *
 * @date 2025/06/26
 */
@Configuration
class LLMFilterProgressMQConfig(
    @Autowired ipUtils: IPUtils
) : AbstractMQConfig(
    exchangeName = EXCHANGE_LLM_FILTER_PROGRESS,
    queueName = "$QUEUE_LLM_FILTER_PROGRESS.${ipUtils.getInnerIPOrHostName()}",
    routeName = ROUTE_LLM_FILTER_PROGRESS
) {
    @Bean
    fun fanoutExchange() = abstractFanoutExchange()

    @Bean
    fun queue() = abstractQueue()

    @Bean
    fun binging(
        @Autowired queue: Queue,
        @Autowired fanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(queue).to(fanoutExchange)
    }

    @Bean
    fun fanoutListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired consumer: LLMFilterProgressMQConsumer
    ) = abstractListenerContainer(
        consumer,
        consumer::consumer.name,
        connectionFactory
    )
}
