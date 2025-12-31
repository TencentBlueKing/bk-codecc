package com.tencent.bk.codecc.task.config

import com.tencent.bk.codecc.task.consumer.RefreshAdminPrivilegeConsumer
import com.tencent.codecc.common.mq.AbstractMQConfig
import com.tencent.devops.common.web.mq.EXCHANGE_ADMIN_PRIVILEGE_REFRESH
import com.tencent.devops.common.web.mq.QUEUE_ADMIN_PRIVILEGE_REFRESH
import com.tencent.devops.common.web.mq.ROUTE_ADMIN_PRIVILEGE_REFRESH
import org.springframework.amqp.core.CustomExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RefreshAdminPrivilegeMQConfig : AbstractMQConfig(
    exchangeName = EXCHANGE_ADMIN_PRIVILEGE_REFRESH,
    queueName = QUEUE_ADMIN_PRIVILEGE_REFRESH,
    routeName = ROUTE_ADMIN_PRIVILEGE_REFRESH
) {
    @Bean
    fun refreshAdminPrivilegeExchange() = abstractExchange()

    @Bean
    fun refreshAdminPrivilegeQueue() = abstractQueue()

    @Bean
    fun refreshAdminPrivilegeBinding(
        @Autowired refreshAdminPrivilegeExchange: CustomExchange,
        @Autowired refreshAdminPrivilegeQueue: Queue
    ) = abstractBinding(refreshAdminPrivilegeQueue, refreshAdminPrivilegeExchange)

    @Bean
    fun refreshAdminPrivilegeQueueListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired refreshAdminPrivilegeConsumer: RefreshAdminPrivilegeConsumer
    ) = abstractListenerContainer(
        refreshAdminPrivilegeConsumer,
        refreshAdminPrivilegeConsumer::consumer.name,
        connectionFactory
    )
}
