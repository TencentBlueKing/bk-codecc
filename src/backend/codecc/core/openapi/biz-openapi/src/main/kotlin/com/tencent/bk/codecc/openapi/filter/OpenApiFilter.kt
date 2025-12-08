package com.tencent.bk.codecc.openapi.filter

import com.tencent.bk.codecc.openapi.exception.PermissionForbiddenException
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.common.web.security.AuthCodeCCToken
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ResourceInfo
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.ext.Provider


@Provider
@RequestFilter
class OpenApiFilter : ContainerRequestFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(OpenApiFilter::class.java)
        // 校验token
        private const val HEADER_TOKEN_KEY = "X-CODECC-OPENAPI-TOKEN"
    }

    @Value("\${codecc.openapi.token:#{null}}")
    private val openApiToken: String? = null

    // JAX-RS 标准中的注释，用于注入与请求相关的上下文对象
    @Context
    lateinit var resourceInfo: ResourceInfo

    override fun filter(requestContext: ContainerRequestContext) {
        val method = resourceInfo.resourceMethod
        val methodHasAnnotation = method.isAnnotationPresent(AuthCodeCCToken::class.java)

        val clazz = resourceInfo.resourceClass
        val clazzHasAnnotation = clazz.isAnnotationPresent(AuthCodeCCToken::class.java)
        // 方法和类都没该注解则放行
        if (!methodHasAnnotation && !clazzHasAnnotation) {
            return
        }

        val reqHeaderToken = requestContext.getHeaderString(HEADER_TOKEN_KEY)
        logger.info("uri is openapi, req header token length: ${reqHeaderToken?.length}")

        if (!openApiToken.isNullOrEmpty() && openApiToken != reqHeaderToken) {
            logger.error("OpenAPI token verification failed")
            throw PermissionForbiddenException("OpenAPI token verification failed")
        }
    }
}