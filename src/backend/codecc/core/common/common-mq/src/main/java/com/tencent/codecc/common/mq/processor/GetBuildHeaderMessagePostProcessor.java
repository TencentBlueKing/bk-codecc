package com.tencent.codecc.common.mq.processor;

import static com.tencent.devops.common.api.auth.HeaderKt.TRACE_HEADER_BUILD_ID;

import com.tencent.devops.common.util.TraceBuildIdThreadCacheUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;

/**
 * 获取 TRACE-BUILD-ID 请求头
 */
public class GetBuildHeaderMessagePostProcessor implements CodeCCAfterMessagePostProcessor {

    @Override
    public Message postProcessMessage(Message message) throws AmqpException {
        String buildId = (String) message.getMessageProperties().getHeaders().get(TRACE_HEADER_BUILD_ID);
        if (StringUtils.isNotBlank(buildId)) {
            TraceBuildIdThreadCacheUtils.INSTANCE.setBuildId(buildId);
        } else {
            TraceBuildIdThreadCacheUtils.INSTANCE.removeBuildId();
        }
        return message;
    }
}