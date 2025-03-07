package com.tencent.bk.codecc.defect.config;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_TASK_INVALID_TOOL_DEFECT;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_TASK_INVALID_TOOL_DEFECT;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_TASK_INVALID_TOOL_DEFECT;

import com.tencent.bk.codecc.defect.condition.AsyncReportCondition;
import com.tencent.bk.codecc.defect.consumer.TaskInvalidToolDefectConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;


@Configuration
@Slf4j
@Conditional(AsyncReportCondition.class)
public class TaskInvalidToolDefectRabbitMQConfig {
    @Bean
    public RabbitAdmin taskInvalidToolDefectRabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Queue taskInvalidToolDefectQueue() {
        return new Queue(QUEUE_TASK_INVALID_TOOL_DEFECT);
    }

    @Bean
    public DirectExchange taskInvalidToolDefectExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_TASK_INVALID_TOOL_DEFECT);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding taskInvalidToolDefectQueueBind(Queue taskInvalidToolDefectQueue,
            DirectExchange taskInvalidToolDefectExchange) {
        return BindingBuilder.bind(taskInvalidToolDefectQueue).to(taskInvalidToolDefectExchange)
                .with(ROUTE_TASK_INVALID_TOOL_DEFECT);
    }

    /**
     * 手动注册容器
     *
     * @param connectionFactory
     */
    @Bean
    public SimpleMessageListenerContainer taskInvalidToolDefectMessageListenerContainer(
            ConnectionFactory connectionFactory,
            Queue taskInvalidToolDefectQueue,
            RabbitAdmin taskInvalidToolDefectRabbitAdmin,
            TaskInvalidToolDefectConsumer taskInvalidToolDefectExcludeConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(taskInvalidToolDefectQueue.getName());
        container.setConcurrentConsumers(20);
        container.setMaxConcurrentConsumers(20);
        container.setPrefetchCount(1);
        container.setAmqpAdmin(taskInvalidToolDefectRabbitAdmin);
        container.setStartConsumerMinInterval(10000);
        container.setConsecutiveActiveTrigger(10);
        MessageListenerAdapter adapter = new MessageListenerAdapter(taskInvalidToolDefectExcludeConsumer, "consumer");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);
        return container;
    }
}
