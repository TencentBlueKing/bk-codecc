package com.tencent.bk.codecc.defect.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.defect.component.ExpireTaskHandleComponent
import com.tencent.bk.codecc.defect.component.PipelineBuildEndHandleComponent
import com.tencent.bk.codecc.defect.component.PluginErrorHandleComponent
import com.tencent.bk.codecc.defect.condition.DefectCondition
import com.tencent.devops.common.web.mq.EXCHANGE_EXPIRED_TASK_STATUS
import com.tencent.devops.common.web.mq.EXCHANGE_PIPELINE_BUILD_END_CALLBACK
import com.tencent.devops.common.web.mq.EXCHANGE_PLUGIN_ERROR_CALLBACK
import com.tencent.devops.common.web.mq.QUEUE_EXPIRED_TASK_STATUS
import com.tencent.devops.common.web.mq.QUEUE_PIPELINE_BUILD_END_CALLBACK
import com.tencent.devops.common.web.mq.QUEUE_PLUGIN_ERROR_CALLBACK
import com.tencent.devops.common.web.mq.ROUTE_EXPIRED_TASK_STATUS
import com.tencent.devops.common.web.mq.ROUTE_PIPELINE_BUILD_END_CALLBACK
import com.tencent.devops.common.web.mq.ROUTE_PLUGIN_ERROR_CALLBACK
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.CustomExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration

@Conditional(DefectCondition::class)
@Configuration
class CommonMessageQueueConfig {

    @Bean
    fun rabbitAdmin(@Autowired connectionFactory: ConnectionFactory): RabbitAdmin =
        RabbitAdmin(connectionFactory)

    @Bean
    fun expiredTaskExchange(): CustomExchange {
        return CustomExchange(
            EXCHANGE_EXPIRED_TASK_STATUS, "x-delayed-message", true,
            false, mapOf("x-delayed-type" to "direct")
        )
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    fun expiredTaskQueue() = Queue(QUEUE_EXPIRED_TASK_STATUS)

    @Bean
    fun expiredTaskQueueBind(
        @Autowired expiredTaskQueue: Queue,
        @Autowired expiredTaskExchange: CustomExchange
    ): Binding =
        BindingBuilder.bind(expiredTaskQueue).to(expiredTaskExchange).with(ROUTE_EXPIRED_TASK_STATUS).noargs()

    @Bean
    fun expiredTaskListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired expiredTaskQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired expiredTaskHandleComponent: ExpireTaskHandleComponent,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(expiredTaskQueue.name)
        container.setConcurrentConsumers(1)
        container.setMaxConcurrentConsumers(5)
        container.setStartConsumerMinInterval(1)
        container.setConsecutiveActiveTrigger(1)
        container.setPrefetchCount(1)
        container.setAmqpAdmin(rabbitAdmin)
        // 确保只有一个消费者消费，保证负载不超时
        val adapter = MessageListenerAdapter(
            expiredTaskHandleComponent,
            expiredTaskHandleComponent::updateExpiredTaskStatus.name
        )
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }

    @Bean
    fun pipelineBuildEndExchange(): CustomExchange {
        return CustomExchange(
            EXCHANGE_PIPELINE_BUILD_END_CALLBACK, "x-delayed-message", true,
            false, mapOf("x-delayed-type" to "direct")
        )
    }

    @Bean
    fun pipelineBuildEndQueue() = Queue(QUEUE_PIPELINE_BUILD_END_CALLBACK)

    @Bean
    fun pipelineBuildEndQueueBind(
        @Autowired pipelineBuildEndQueue: Queue,
        @Autowired pipelineBuildEndExchange: CustomExchange
    ): Binding = BindingBuilder.bind(pipelineBuildEndQueue).to(pipelineBuildEndExchange)
            .with(ROUTE_PIPELINE_BUILD_END_CALLBACK).noargs()

    @Bean
    fun pipelineBuildEndListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildEndQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired pipelineBuildEndHandleComponent: PipelineBuildEndHandleComponent,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(pipelineBuildEndQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(5)
        container.setStartConsumerMinInterval(1)
        container.setConsecutiveActiveTrigger(1)
        container.setPrefetchCount(1)
        container.setAmqpAdmin(rabbitAdmin)
        // 确保只有一个消费者消费，保证负载不超时
        val adapter = MessageListenerAdapter(
            pipelineBuildEndHandleComponent,
            pipelineBuildEndHandleComponent::handlePipelineBuildEndCallback.name
        )
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }

    @Bean
    fun pluginErrorExchange(): CustomExchange {
        return CustomExchange(
            EXCHANGE_PLUGIN_ERROR_CALLBACK, "x-delayed-message", true,
            false, mapOf("x-delayed-type" to "direct")
        )
    }

    @Bean
    fun pluginErrorQueue() = Queue(QUEUE_PLUGIN_ERROR_CALLBACK)

    @Bean
    fun pluginErrorQueueBind(
        @Autowired pluginErrorQueue: Queue,
        @Autowired pluginErrorExchange: CustomExchange
    ): Binding =
        BindingBuilder.bind(pluginErrorQueue).to(pluginErrorExchange).with(ROUTE_PLUGIN_ERROR_CALLBACK).noargs()

    @Bean
    fun pluginErrorListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pluginErrorQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired pluginErrorHandleComponent: PluginErrorHandleComponent,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(pluginErrorQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(5)
        container.setStartConsumerMinInterval(1)
        container.setConsecutiveActiveTrigger(1)
        container.setPrefetchCount(1)
        container.setAmqpAdmin(rabbitAdmin)
        // 确保只有一个消费者消费，保证负载不超时
        val adapter = MessageListenerAdapter(
            pluginErrorHandleComponent,
            pluginErrorHandleComponent::handlePluginErrorCallback.name
        )
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }
}
