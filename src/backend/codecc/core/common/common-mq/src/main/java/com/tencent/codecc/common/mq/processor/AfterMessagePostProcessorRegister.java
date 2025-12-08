package com.tencent.codecc.common.mq.processor;

import java.util.Map;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * MQ消息 消费前增强注册器，主要注册到MessageListenerContainer中
 */
public class AfterMessagePostProcessorRegister implements ApplicationContextAware {


    private ApplicationContext applicationContext;

    /**
     * 注册CodeCCAfterMessagePostProcessor实例处理器
     */
    @PostConstruct
    public void registerProcessor() {
        Map<String, MessageListenerContainer> nameToContainerMap = applicationContext.getBeansOfType(
                MessageListenerContainer.class);
        if (nameToContainerMap.isEmpty()) {
            return;
        }
        Map<String, CodeCCAfterMessagePostProcessor> nameToProcessorMap = applicationContext.getBeansOfType(
                CodeCCAfterMessagePostProcessor.class);
        if (nameToProcessorMap.isEmpty()) {
            return;
        }
        nameToContainerMap.values().forEach(container -> {
            if (container instanceof AbstractMessageListenerContainer) {
                nameToProcessorMap.values().forEach(processor -> {
                    ((AbstractMessageListenerContainer) container).addAfterReceivePostProcessors(processor);
                });
            }
        });
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
