package com.tencent.bk.codecc.defect.config;

import com.tencent.bk.codecc.defect.condition.AsyncReportCondition;
import com.tencent.bk.codecc.defect.condition.DefectCondition;
import com.tencent.bk.codecc.defect.consumer.ScanSlaConsumer;
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

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_SCAN_FINISH;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_SCAN_FINISH_FOR_SCAN_SLA;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_SCAN_FINISH;


@Configuration
@Slf4j
@Conditional(DefectCondition.class)
public class ScanSlaRabbitMQConfig {
    @Bean
    public RabbitAdmin scanSlaRabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Queue scanSlaQueue() {
        return new Queue(QUEUE_SCAN_FINISH_FOR_SCAN_SLA);
    }

    @Bean
    public CustomExchange scanSlaExchange() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-delayed-type", "direct");
        return new CustomExchange(EXCHANGE_SCAN_FINISH, "x-delayed-message", true,
                false, arguments);
    }

    @Bean
    public Binding scanSlaQueueBind(@Qualifier("scanSlaQueue") Queue scanSlaQueue,
                                     @Qualifier("scanSlaExchange") CustomExchange scanSlaExchange) {
        return BindingBuilder.bind(scanSlaQueue).to(scanSlaExchange).with(ROUTE_SCAN_FINISH).noargs();
    }

    /**
     * 手动注册容器
     *
     * @param connectionFactory
     */
    @Bean
    public SimpleMessageListenerContainer scanSlaMessageListenerContainer(
            ConnectionFactory connectionFactory,
            @Qualifier("scanSlaQueue") Queue scanSlaQueue,
            @Qualifier("scanSlaRabbitAdmin") RabbitAdmin scanSlaRabbitAdmin,
            ScanSlaConsumer scanSlaConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(scanSlaQueue.getName());
        container.setConcurrentConsumers(10);
        container.setMaxConcurrentConsumers(10);
        container.setPrefetchCount(1);
        container.setAmqpAdmin(scanSlaRabbitAdmin);
        container.setStartConsumerMinInterval(10000);
        container.setConsecutiveActiveTrigger(10);
        MessageListenerAdapter adapter = new MessageListenerAdapter(scanSlaConsumer, "consumer");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);
        return container;
    }
}
