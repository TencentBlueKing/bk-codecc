package com.tencent.bk.codecc.defect.config;

import com.tencent.bk.codecc.defect.condition.AsyncReportCondition;
import com.tencent.bk.codecc.defect.consumer.CommonDefectMigrationConsumer;
import com.tencent.devops.common.service.IConsumer;
import com.tencent.devops.common.web.mq.ConstantsKt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * 告警数据迁移MQ配置
 */
@Configuration
@Slf4j
@Conditional(AsyncReportCondition.class)
public class DefectMigrationRabbitMQConfig {

    @Bean
    public DirectExchange commonDefectMigrationDirectExchange() {
        DirectExchange directExchange = new DirectExchange(ConstantsKt.EXCHANGE_DEFECT_MIGRATION_COMMON);
        directExchange.setDelayed(true);

        return directExchange;
    }

    @Bean
    public Queue commonDefectMigrationQueue() {
        return new Queue(ConstantsKt.QUEUE_DEFECT_MIGRATION_COMMON);
    }

    @Bean
    public Binding commonDefectMigrationQueueBind(
            Queue commonDefectMigrationQueue,
            DirectExchange commonDefectMigrationDirectExchange
    ) {
        return BindingBuilder.bind(commonDefectMigrationQueue)
                .to(commonDefectMigrationDirectExchange)
                .with(ConstantsKt.ROUTE_DEFECT_MIGRATION_COMMON);
    }

    @Bean
    public SimpleMessageListenerContainer commonDefectMigrationMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CommonDefectMigrationConsumer commonDefectMigrationConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter
    ) {
        return getSimpleMessageListenerContainer(
                ConstantsKt.QUEUE_DEFECT_MIGRATION_COMMON,
                commonDefectMigrationConsumer,
                connectionFactory, jackson2JsonMessageConverter
        );
    }

    protected SimpleMessageListenerContainer getSimpleMessageListenerContainer(
            String queueName,
            IConsumer consumerImpl,
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter
    ) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setConcurrentConsumers(10);
        container.setMaxConcurrentConsumers(10);
        container.setStartConsumerMinInterval(10000);
        container.setConsecutiveActiveTrigger(10);
        container.setPrefetchCount(1);
        MessageListenerAdapter adapter = new MessageListenerAdapter(consumerImpl, "consumer");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);

        return container;
    }
}
