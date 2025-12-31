package com.tencent.bk.codecc.defect.config;

import com.tencent.bk.codecc.defect.condition.DefectCondition;
import com.tencent.bk.codecc.defect.consumer.IgnoreTypeNotifyConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_IGNORE_TYPE_NOTIFY;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_IGNORE_TYPE_NOTIFY;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_IGNORE_TYPE_NOTIFY;


@Configuration
@Slf4j
@Conditional(DefectCondition.class)
public class IgnoreTypeNotifyRabbitMQConfig {
    @Bean
    public RabbitAdmin ignoreTypeNotifyRabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Queue ignoreTypeNotifyQueue() {
        return new Queue(QUEUE_IGNORE_TYPE_NOTIFY);
    }

    @Bean
    public CustomExchange ignoreTypeNotifyExchange() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-delayed-type", "direct");
        return new CustomExchange(EXCHANGE_IGNORE_TYPE_NOTIFY, "x-delayed-message", true,
                false, arguments);
    }

    @Bean
    public Binding ignoreTypeNotifyQueueBind(@Qualifier("ignoreTypeNotifyQueue") Queue ignoreTypeNotifyQueue,
                                              @Qualifier("ignoreTypeNotifyExchange") CustomExchange ignoreTypeNotifyExchange) {
        return BindingBuilder.bind(ignoreTypeNotifyQueue).to(ignoreTypeNotifyExchange)
                .with(ROUTE_IGNORE_TYPE_NOTIFY).noargs();
    }

    /**
     * 手动注册容器
     *
     * @param connectionFactory
     */
    @Bean
    public SimpleMessageListenerContainer ignoreTypeNotifyMessageListenerContainer(
            ConnectionFactory connectionFactory,
            @Qualifier("ignoreTypeNotifyQueue") Queue ignoreTypeNotifyQueue,
            @Qualifier("ignoreTypeNotifyRabbitAdmin") RabbitAdmin ignoreTypeNotifyRabbitAdmin,
            IgnoreTypeNotifyConsumer ignoreTypeNotifyConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(ignoreTypeNotifyQueue.getName());
        container.setConcurrentConsumers(5);
        container.setMaxConcurrentConsumers(5);
        container.setPrefetchCount(1);
        container.setAmqpAdmin(ignoreTypeNotifyRabbitAdmin);
        container.setStartConsumerMinInterval(10000);
        container.setConsecutiveActiveTrigger(10);
        MessageListenerAdapter adapter = new MessageListenerAdapter(ignoreTypeNotifyConsumer,
                "consumer");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);
        return container;
    }
}
