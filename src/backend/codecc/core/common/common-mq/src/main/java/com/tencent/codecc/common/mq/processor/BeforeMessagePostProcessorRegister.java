package com.tencent.codecc.common.mq.processor;

import java.util.Map;
import javax.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * MQ 消息发送前增强注册器 主要注册到RabbitTemplate中
 */
public class BeforeMessagePostProcessorRegister implements ApplicationContextAware {


    private ApplicationContext applicationContext;

    /**
     * 注册CodeCCBeforeMessagePostProcessor实例处理器
     */
    @PostConstruct
    public void registerProcessor() {
        Map<String, RabbitTemplate> nameToTemplateMap = applicationContext.getBeansOfType(
                RabbitTemplate.class);
        if (nameToTemplateMap.isEmpty()) {
            return;
        }
        Map<String, CodeCCBeforeMessagePostProcessor> nameToProcessorMap = applicationContext.getBeansOfType(
                CodeCCBeforeMessagePostProcessor.class);
        if (nameToProcessorMap.isEmpty()) {
            return;
        }
        nameToTemplateMap.values().forEach(template -> {
            nameToProcessorMap.values().forEach(template::addAfterReceivePostProcessors);
        });
    }


    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
