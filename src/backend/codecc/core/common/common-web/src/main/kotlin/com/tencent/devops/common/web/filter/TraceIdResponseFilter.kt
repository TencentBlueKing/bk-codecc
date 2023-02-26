package com.tencent.devops.common.web.filter

import com.tencent.devops.common.web.ResponseFilter
import io.opentelemetry.api.trace.Span
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.ext.Provider

@Provider
@ResponseFilter
class TraceIdResponseFilter : ContainerResponseFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(TraceIdResponseFilter::class.java)
    }

    override fun filter(requestContext: ContainerRequestContext?, responseContext: ContainerResponseContext?) {
        val traceId = Span.current().spanContext.traceId
        if (StringUtils.isBlank(traceId) || responseContext!!.headers.isEmpty()) {
            logger.error("TraceIdHeaderFilter : traceId:{}", traceId)
            return
        }
        responseContext.headers["Trace-ID"] = listOf(traceId)
    }
}