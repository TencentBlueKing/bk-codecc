package com.tencent.devops.common.web.filter

import com.tencent.devops.common.util.TraceBuildIdThreadCacheUtils
import com.tencent.devops.common.web.ResponseFilter
import org.slf4j.LoggerFactory
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.ext.Provider

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