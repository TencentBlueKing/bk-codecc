package com.tencent.bk.codecc.defect.config

import com.tencent.bk.codecc.defect.consumer.LLMNegativeDefectLearnConsumer
import com.tencent.codecc.common.mq.AbstractMQConfig
import com.tencent.devops.common.web.mq.EXCHANGE_LLM_NEGATIVE_DEFECT_LEARN
import com.tencent.devops.common.web.mq.QUEUE_LLM_NEGATIVE_DEFECT_LEARN
import com.tencent.devops.common.web.mq.ROUTE_LLM_NEGATIVE_DEFECT_LEARN
import org.springframework.amqp.core.CustomExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LLMNegativeDefectLearnMQConfig : AbstractMQConfig(
    exchangeName = EXCHANGE_LLM_NEGATIVE_DEFECT_LEARN,
    queueName = QUEUE_LLM_NEGATIVE_DEFECT_LEARN,
    routeName = ROUTE_LLM_NEGATIVE_DEFECT_LEARN
) {
    @Bean
    fun llmNegativeDefectLearnExchange() = abstractExchange()

    @Bean
    fun llmNegativeDefectLearnQueue() = abstractQueue()

    @Bean
    fun llmNegativeDefectLearnBinding(
        @Autowired llmNegativeDefectLearnExchange: CustomExchange,
        @Autowired llmNegativeDefectLearnQueue: Queue
    ) = abstractBinding(llmNegativeDefectLearnQueue, llmNegativeDefectLearnExchange)

    @Bean
    fun llmNegativeDefectLearnListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired llmNegativeDefectLearnConsumer: LLMNegativeDefectLearnConsumer
    ) = abstractNoAckListenerContainer(
        llmNegativeDefectLearnConsumer,
        llmNegativeDefectLearnConsumer::consumer.name,
        connectionFactory
    )
}
