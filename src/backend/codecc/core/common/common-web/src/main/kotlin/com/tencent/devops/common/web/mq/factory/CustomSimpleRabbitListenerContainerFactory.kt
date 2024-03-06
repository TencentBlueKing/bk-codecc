package com.tencent.devops.common.web.mq.factory

import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer

class CustomSimpleRabbitListenerContainerFactory(
    private vararg val messagePostProcessor: MessagePostProcessor
) : SimpleRabbitListenerContainerFactory() {
    override fun initializeContainer(instance: SimpleMessageListenerContainer?, endpoint: RabbitListenerEndpoint?) {
        super.initializeContainer(instance, endpoint)
        instance?.let { container ->
            messagePostProcessor.forEach {
                container.addAfterReceivePostProcessors(it)
            }
        }
    }
}
