package com.tencent.codecc.common.mq;

import com.tencent.codecc.common.mq.processor.AfterMessagePostProcessorRegister;
import com.tencent.codecc.common.mq.processor.BeforeMessagePostProcessorRegister;
import com.tencent.codecc.common.mq.processor.GetBuildHeaderMessagePostProcessor;
import com.tencent.codecc.common.mq.processor.SetBuildHeaderMessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * MQ 相关增强自动注册
 */
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class MqAutoConfiguration {

    /**
     * 设置MQ TraceBuildId的请求头
     *
     * @return
     */
    @Bean
    @ConditionalOnBean(RabbitTemplate.class)
    public SetBuildHeaderMessagePostProcessor setBuildHeaderMessagePostProcessor() {
        return new SetBuildHeaderMessagePostProcessor();
    }

    /**
     * MQ 消息发送前增强注册器
     *
     * @return
     */
    @Bean
    @ConditionalOnBean(RabbitTemplate.class)
    public BeforeMessagePostProcessorRegister beforeMessagePostProcessorRegister() {
        return new BeforeMessagePostProcessorRegister();
    }


    /**
     * 获取 TraceBuildId的请求头 设置到线程中
     *
     * @return
     */
    @Bean
    @ConditionalOnBean(MessageListenerContainer.class)
    public GetBuildHeaderMessagePostProcessor getBuildHeaderMessagePostProcessor() {
        return new GetBuildHeaderMessagePostProcessor();
    }

    /**
     * MQ 消息，消费前增强注册器
     *
     * @return
     */
    @Bean
    @ConditionalOnBean(MessageListenerContainer.class)
    public AfterMessagePostProcessorRegister afterMessagePostProcessorRegister() {
        return new AfterMessagePostProcessorRegister();
    }

}
