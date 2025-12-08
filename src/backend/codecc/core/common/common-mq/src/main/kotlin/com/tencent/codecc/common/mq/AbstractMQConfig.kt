package com.tencent.codecc.common.mq

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.CustomExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter

open class AbstractMQConfig(
    private val exchangeName: String,
    private val queueName: String,
    private val routeName: String
) {

    private fun genSimpleMessageListenerContainer(
        inConnectionFactory: ConnectionFactory,
        concurrentConsumers: Int,
        maxConcurrentConsumers: Int,
        startConsumerMinInterval: Long,
        consecutiveActiveTrigger: Int,
        prefetchCount: Int
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(inConnectionFactory)
        container.setQueueNames(queueName)
        container.setConcurrentConsumers(concurrentConsumers)
        container.setMaxConcurrentConsumers(maxConcurrentConsumers)
        container.setStartConsumerMinInterval(startConsumerMinInterval)
        container.setConsecutiveActiveTrigger(consecutiveActiveTrigger)
        container.setPrefetchCount(prefetchCount)

        return container
    }

    fun abstractExchange(
        durable: Boolean = true,
        autoDelete: Boolean = false,
        arguments: Map<String, Any> = mapOf("x-delayed-type" to "direct")
    ) = CustomExchange(exchangeName, "x-delayed-message", durable, autoDelete, arguments)

    fun abstractFanoutExchange(
        durable: Boolean = true,
        autoDelete: Boolean = false
    ) = FanoutExchange(exchangeName, durable, autoDelete)

    fun abstractQueue(name: String = queueName) = Queue(name)

    fun abstractBinding(queue: Queue, exchange: CustomExchange): Binding =
        BindingBuilder.bind(queue).to(exchange).with(routeName).noargs()

    fun abstractListenerContainer(
        inConnectionFactory: ConnectionFactory,
        inMessageConverter: MessageConverter? = null,
        concurrentConsumers: Int = 20,
        maxConcurrentConsumers: Int = 20,
        startConsumerMinInterval: Long = 10000,
        consecutiveActiveTrigger: Int = 20,
        prefetchCount: Int = 1
    ): SimpleMessageListenerContainer {
        val container = genSimpleMessageListenerContainer(
            inConnectionFactory,
            concurrentConsumers,
            maxConcurrentConsumers,
            startConsumerMinInterval,
            consecutiveActiveTrigger,
            prefetchCount
        )

        val messageConverter = inMessageConverter ?: Jackson2JsonMessageConverter(ObjectMapper())
        val adapter = MessageListenerAdapter()

        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)

        return container
    }

    fun abstractNoAckListenerContainer(
        consumer: Any,
        consumerMethod: String,
        inConnectionFactory: ConnectionFactory,
        inMessageConverter: MessageConverter? = null,
        concurrentConsumers: Int = 20,
        maxConcurrentConsumers: Int = 20,
        startConsumerMinInterval: Long = 10000,
        consecutiveActiveTrigger: Int = 20,
        prefetchCount: Int = 1
    ): SimpleMessageListenerContainer {
        return abstractListenerContainer(
            consumer = consumer,
            consumerMethod = consumerMethod,
            inConnectionFactory = inConnectionFactory,
            inMessageConverter = inMessageConverter,
            concurrentConsumers = concurrentConsumers,
            maxConcurrentConsumers = maxConcurrentConsumers,
            startConsumerMinInterval = startConsumerMinInterval,
            consecutiveActiveTrigger = consecutiveActiveTrigger,
            prefetchCount = prefetchCount
        ).apply {
            acknowledgeMode = AcknowledgeMode.NONE
        }
    }

    fun abstractListenerContainer(
        consumer: Any,
        consumerMethod: String,
        inConnectionFactory: ConnectionFactory,
        inMessageConverter: MessageConverter? = null,
        concurrentConsumers: Int = 20,
        maxConcurrentConsumers: Int = 20,
        startConsumerMinInterval: Long = 10000,
        consecutiveActiveTrigger: Int = 20,
        prefetchCount: Int = 1
    ): SimpleMessageListenerContainer {
        val container = genSimpleMessageListenerContainer(
            inConnectionFactory,
            concurrentConsumers,
            maxConcurrentConsumers,
            startConsumerMinInterval,
            consecutiveActiveTrigger,
            prefetchCount
        )

        val messageConverter = inMessageConverter ?: Jackson2JsonMessageConverter(ObjectMapper())
        val adapter = if (consumerMethod.isNotBlank()) {
            MessageListenerAdapter(consumer, consumerMethod)
        } else {
            MessageListenerAdapter()
        }
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)

        return container
    }
}
