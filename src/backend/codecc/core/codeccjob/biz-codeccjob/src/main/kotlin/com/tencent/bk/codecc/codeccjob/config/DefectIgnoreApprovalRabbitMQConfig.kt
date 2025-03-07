package com.tencent.bk.codecc.codeccjob.config

import com.tencent.bk.codecc.codeccjob.consumer.DefectIgnoreApprovalConsumer
import com.tencent.codecc.common.mq.AbstractMQConfig
import com.tencent.devops.common.web.mq.EXCHANGE_CODECC_DEFECT_IGNORE_APPROVAL
import com.tencent.devops.common.web.mq.QUEUE_CODECC_DEFECT_IGNORE_APPROVAL
import com.tencent.devops.common.web.mq.ROUTE_CODECC_DEFECT_IGNORE_APPROVAL
import org.springframework.amqp.core.CustomExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DefectIgnoreApprovalRabbitMQConfig : AbstractMQConfig(
    exchangeName = EXCHANGE_CODECC_DEFECT_IGNORE_APPROVAL,
    queueName = QUEUE_CODECC_DEFECT_IGNORE_APPROVAL,
    routeName = ROUTE_CODECC_DEFECT_IGNORE_APPROVAL
) {

    @Bean
    fun defectIgnoreApprovalExchange() = abstractExchange()

    @Bean
    fun defectIgnoreApprovalQueue() = abstractQueue()

    @Bean
    fun defectIgnoreApprovalBinding(
        @Autowired defectIgnoreApprovalExchange: CustomExchange,
        @Autowired defectIgnoreApprovalQueue: Queue
    ) = abstractBinding(defectIgnoreApprovalQueue, defectIgnoreApprovalExchange)

    @Bean
    fun defectIgnoreApprovalListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired defectIgnoreApprovalConsumer: DefectIgnoreApprovalConsumer
    ) = abstractListenerContainer(
        defectIgnoreApprovalConsumer,
        defectIgnoreApprovalConsumer::consumer.name,
        connectionFactory
    )
}
