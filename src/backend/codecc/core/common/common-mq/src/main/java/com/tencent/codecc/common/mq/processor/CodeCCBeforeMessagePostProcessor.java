package com.tencent.codecc.common.mq.processor;

import org.springframework.amqp.core.MessagePostProcessor;

/**
 * MQ发送前处理器
 */
public interface CodeCCBeforeMessagePostProcessor extends MessagePostProcessor {

}