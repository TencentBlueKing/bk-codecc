package com.tencent.bk.codecc.codeccjob.config;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_DATA_SEPARATION;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_DATA_SEPARATION_COOL_DOWN;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_DATA_SEPARATION_COOL_DOWN_TRIGGER;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_DATA_SEPARATION_WARM_UP;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DATA_SEPARATION_COOL_DOWN;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DATA_SEPARATION_COOL_DOWN_TRIGGER;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DATA_SEPARATION_WARM_UP;

import com.tencent.bk.codecc.codeccjob.consumer.DataSeparationCoolDownConsumer;
import com.tencent.bk.codecc.codeccjob.consumer.DataSeparationCoolDownTriggerConsumer;
import com.tencent.bk.codecc.codeccjob.consumer.DataSeparationWarmUpConsumer;
import com.tencent.devops.common.service.IConsumer;
import org.jetbrains.annotations.NotNull;
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
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeparationRabbitMQConfig {

    private static final int DEFAULT_CONCURRENCY = 6;

    @Bean
    public DirectExchange commonDataSeparationDirectExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DATA_SEPARATION);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Queue coolDownQueue() {
        return new Queue(QUEUE_DATA_SEPARATION_COOL_DOWN);
    }

    @Bean
    public Binding coolDownQueueBind(
            Queue coolDownQueue,
            DirectExchange commonDataSeparationDirectExchange
    ) {
        return BindingBuilder.bind(coolDownQueue)
                .to(commonDataSeparationDirectExchange)
                .with(ROUTE_DATA_SEPARATION_COOL_DOWN);
    }

    @Bean
    public SimpleMessageListenerContainer coolDownMessageListenerContainer(
            ConnectionFactory connectionFactory,
            DataSeparationCoolDownConsumer dataSeparationCoolDownConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter
    ) {
        return getSimpleMessageListenerContainer(
                QUEUE_DATA_SEPARATION_COOL_DOWN,
                dataSeparationCoolDownConsumer,
                connectionFactory,
                jackson2JsonMessageConverter,
                DEFAULT_CONCURRENCY
        );
    }

    @Bean
    public Queue warmUpQueue() {
        return new Queue(QUEUE_DATA_SEPARATION_WARM_UP);
    }

    @Bean
    public Binding warmUpQueueBind(
            Queue warmUpQueue,
            DirectExchange commonDataSeparationDirectExchange
    ) {
        return BindingBuilder.bind(warmUpQueue)
                .to(commonDataSeparationDirectExchange)
                .with(ROUTE_DATA_SEPARATION_WARM_UP);
    }

    @Bean
    public SimpleMessageListenerContainer warmUpMessageListenerContainer(
            ConnectionFactory connectionFactory,
            DataSeparationWarmUpConsumer dataSeparationWarmUpConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter
    ) {
        return getSimpleMessageListenerContainer(
                QUEUE_DATA_SEPARATION_WARM_UP,
                dataSeparationWarmUpConsumer,
                connectionFactory,
                jackson2JsonMessageConverter,
                DEFAULT_CONCURRENCY
        );
    }

    @Bean
    public Queue coolDownTriggerQueue() {
        return new Queue(QUEUE_DATA_SEPARATION_COOL_DOWN_TRIGGER);
    }

    @Bean
    public Binding coolDownTriggerQueueBind(
            Queue coolDownTriggerQueue,
            DirectExchange commonDataSeparationDirectExchange
    ) {
        return BindingBuilder.bind(coolDownTriggerQueue)
                .to(commonDataSeparationDirectExchange)
                .with(ROUTE_DATA_SEPARATION_COOL_DOWN_TRIGGER);
    }

    @Bean
    public SimpleMessageListenerContainer coolDownTriggerQueueMessageListenerContainer(
            ConnectionFactory connectionFactory,
            DataSeparationCoolDownTriggerConsumer dataSeparationCoolDownTriggerConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter
    ) {
        return getSimpleMessageListenerContainer(
                QUEUE_DATA_SEPARATION_COOL_DOWN_TRIGGER,
                dataSeparationCoolDownTriggerConsumer,
                connectionFactory,
                jackson2JsonMessageConverter,
                1
        );
    }

    @NotNull
    private SimpleMessageListenerContainer getSimpleMessageListenerContainer(
            String queueName,
            IConsumer consumer,
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            Integer concurrency
    ) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setConcurrentConsumers(concurrency);
        container.setMaxConcurrentConsumers(concurrency);
        container.setStartConsumerMinInterval(10000);
        container.setConsecutiveActiveTrigger(concurrency);
        container.setPrefetchCount(1);
        container.setAmqpAdmin(new RabbitAdmin(connectionFactory));
        MessageListenerAdapter adapter = new MessageListenerAdapter(consumer, "consumer");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);

        return container;
    }
}
