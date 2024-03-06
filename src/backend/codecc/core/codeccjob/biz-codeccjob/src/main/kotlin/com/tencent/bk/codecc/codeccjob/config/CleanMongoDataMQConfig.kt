package com.tencent.bk.codecc.codeccjob.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.codeccjob.consumer.CleanMongoDataConsumer
import com.tencent.devops.common.service.utils.IPUtils
import com.tencent.devops.common.web.mq.EXCHANGE_CLEAN_MONGO_DATA
import com.tencent.devops.common.web.mq.QUEUE_CLEAN_MONGO_DATA
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CleanMongoDataMQConfig {

    @Value("\${server.port:#{null}}")
    private val localPort: String? = null

    @Bean
    fun cleanMongoDataQueue(@Autowired ipUtils: IPUtils): Queue {
        val queueName = "$QUEUE_CLEAN_MONGO_DATA.${ipUtils.getInnerIPOrHostName()}.$localPort"
        return Queue(queueName)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    fun cleanMongoDataExchange(): FanoutExchange {
        return FanoutExchange(EXCHANGE_CLEAN_MONGO_DATA, false, true)
    }

    @Bean
    fun cleanMongoDataQueueBind(
        @Autowired cleanMongoDataQueue: Queue,
        @Autowired cleanMongoDataExchange: FanoutExchange): Binding {
        return BindingBuilder.bind(cleanMongoDataQueue).to(cleanMongoDataExchange)
    }

    @Bean
    fun toolMetaCacheMessageListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired cleanMongoDataQueue: Queue,
        @Autowired cleanMongoDataConsumer: CleanMongoDataConsumer,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.connectionFactory = connectionFactory
        container.setQueueNames(cleanMongoDataQueue.name)
        container.setPrefetchCount(1)
        val adapter = MessageListenerAdapter(cleanMongoDataConsumer, "consumer")
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }
}