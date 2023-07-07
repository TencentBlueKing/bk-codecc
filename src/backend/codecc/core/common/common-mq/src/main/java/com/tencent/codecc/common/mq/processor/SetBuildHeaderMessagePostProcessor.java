package com.tencent.codecc.common.mq.processor;

import static com.tencent.devops.common.api.auth.HeaderKt.TRACE_HEADER_BUILD_ID;

import com.tencent.devops.common.util.TraceBuildIdThreadCacheUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;

/**
 * 设置 TRACE-BUILD-ID 请求头
 */
public class SetBuildHeaderMessagePostProcessor implements CodeCCBeforeMessagePostProcessor {

    @Override
    public Message postProcessMessage(Message message) throws AmqpException {
        String traceBuildId = TraceBuildIdThreadCacheUtils.INSTANCE.getBuildId();
        if (StringUtils.isNotBlank(traceBuildId)) {
            message.getMessageProperties().getHeaders().put(TRACE_HEADER_BUILD_ID, traceBuildId);
        }
        return message;
    }
}
