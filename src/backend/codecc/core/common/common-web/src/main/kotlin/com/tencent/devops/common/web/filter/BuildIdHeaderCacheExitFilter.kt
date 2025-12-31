package com.tencent.devops.common.web.filter

import com.tencent.devops.common.util.TraceBuildIdThreadCacheUtils
import com.tencent.devops.common.web.ResponseFilter
import org.slf4j.LoggerFactory
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider

/**
 * 请求后，清除BuildId缓存
 */
@Provider
@ResponseFilter
class BuildIdHeaderCacheExitFilter : ContainerResponseFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(BuildIdHeaderCacheExitFilter::class.java)
    }

    override fun filter(requestContext: ContainerRequestContext?, responseContext: ContainerResponseContext?) {
        TraceBuildIdThreadCacheUtils.removeBuildId()
    }
}