package com.tencent.devops.common.web.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.TRACE_HEADER_BUILD_ID
import com.tencent.devops.common.util.TraceBuildIdThreadCacheUtils
import com.tencent.devops.common.web.RequestFilter
import org.slf4j.LoggerFactory
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.ext.Provider

/**
 * 请求入口过滤器，为Build接口增加Build_id的Header
 */
@Provider
@RequestFilter
class BuildIdHeaderCacheEnterFilter : ContainerRequestFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(BuildIdHeaderCacheEnterFilter::class.java)
    }

    /**
     * 传递 TRACE_HEADER_BUILD_ID Header
     */
    override fun filter(requestContext: ContainerRequestContext?) {
        //判断是否包含BuildId头部
        val url = requestContext?.uriInfo?.requestUri?.path
        val devopsBuildIdHeader = requestContext?.getHeaderString(AUTH_HEADER_DEVOPS_BUILD_ID)
        val traceBuildIdHeader = requestContext?.getHeaderString(TRACE_HEADER_BUILD_ID)
        // 来自AUTH_HEADER_DEVOPS_BUILD_ID 的优先进行缓存，是TRACE_HEADER_BUILD_ID的原始值
        if (!url.isNullOrBlank() && url.startsWith("/api/build") && !devopsBuildIdHeader.isNullOrEmpty()) {
            TraceBuildIdThreadCacheUtils.setBuildId(devopsBuildIdHeader)
            return
        }
        // TRACE_HEADER_BUILD_ID 来自service接口
        if (!traceBuildIdHeader.isNullOrEmpty()) {
            TraceBuildIdThreadCacheUtils.setBuildId(traceBuildIdHeader)
            return
        }
        TraceBuildIdThreadCacheUtils.removeBuildId()
    }


}