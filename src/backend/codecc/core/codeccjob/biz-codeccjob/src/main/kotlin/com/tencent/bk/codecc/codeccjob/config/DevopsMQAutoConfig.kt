package com.tencent.bk.codecc.codeccjob.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 蓝盾度量MQ
 */
@Configuration
@AutoConfigureBefore(RabbitAutoConfiguration::class)
@ConditionalOnProperty(prefix = "spring.rabbitmq.devops", name = ["addresses", "virtual-host", "username", "password"])
class DevopsMQAutoConfig {

    @Bean(name = ["devopsConnectionFactory"])
    fun devopsConnectionFactory(
        @Value("\${spring.rabbitmq.devops.username:#{null}}") userName: String,
        @Value("\${spring.rabbitmq.devops.password:#{null}}") passWord: String,
        @Value("\${spring.rabbitmq.devops.virtual-host:#{null}}") vHost: String,
        @Value("\${spring.rabbitmq.devops.addresses:#{null}}") address: String
    ): ConnectionFactory {
        val connectionFactory = CachingConnectionFactory()
        connectionFactory.username = userName
        connectionFactory.setPassword(passWord)
        connectionFactory.virtualHost = vHost
        connectionFactory.setAddresses(address)

        return connectionFactory
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    /**
     * 生产者
     */
    @Bean(name= ["devopsRabbitTemplate"])
    fun devopsRabbitTemplate(
        @Qualifier("devopsConnectionFactory") connectionFactory: ConnectionFactory,
        objectMapper: ObjectMapper
    ) : RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = messageConverter(objectMapper)
        return rabbitTemplate
    }
}
