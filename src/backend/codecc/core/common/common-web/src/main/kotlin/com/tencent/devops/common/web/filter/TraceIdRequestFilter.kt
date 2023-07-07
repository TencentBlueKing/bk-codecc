package com.tencent.devops.common.web.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.web.RequestFilter
import io.opentelemetry.api.trace.Span
import org.slf4j.LoggerFactory
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.ext.Provider

/**
 * 为链路添加buildId属性
 */
@Provider
@RequestFilter
class TraceIdRequestFilter : ContainerRequestFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(TraceIdRequestFilter::class.java)
    }

    override fun filter(requestContext: ContainerRequestContext?) {
        //判断是否包含BuildId头部
        val devopsBuildIdHeader = requestContext?.getHeaderString(AUTH_HEADER_DEVOPS_BUILD_ID)
        if (!devopsBuildIdHeader.isNullOrEmpty()) {
            Span.current().setAttribute("build.id", devopsBuildIdHeader)
            return
        }
    }

}