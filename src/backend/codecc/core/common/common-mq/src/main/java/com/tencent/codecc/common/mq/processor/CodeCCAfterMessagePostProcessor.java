package com.tencent.codecc.common.mq.processor;

import org.springframework.amqp.core.MessagePostProcessor;

/**
 * MQ消费前处理器
 */
public interface CodeCCAfterMessagePostProcessor extends MessagePostProcessor {

}