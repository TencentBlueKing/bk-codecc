package com.codecc.preci.api.client

import com.codecc.preci.core.log.PreCILogger
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okio.Buffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * HTTP 请求/响应日志拦截器
 *
 * 用于记录 PreCI API 的 HTTP 请求和响应详情，便于开发和调试。
 * 包含请求方法、URL、请求头、请求体、响应状态码、响应头、响应体等信息。
 *
 * **日志级别：**
 * - INFO：记录请求和响应的基本信息（方法、URL、状态码）
 * - DEBUG：记录详细的请求头、响应头和请求/响应体
 *
 * **注意：** 在生产环境中建议降低日志级别或禁用详细日志，以提高性能和保护敏感信息
 *
 * @since 1.0
 */
class LoggingInterceptor : Interceptor {
    private val logger = PreCILogger.getLogger(LoggingInterceptor::class.java)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 记录请求信息
        logger.info("--> ${request.method} ${request.url}")

        if (logger.isDebugEnabled()) {
            // 记录请求头
            request.headers.forEach { (name, value) ->
                logger.debug("  $name: $value")
            }

            // 记录请求体
            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                val charset = body.contentType()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
                val content = buffer.readString(charset)
                logger.debug("  Body: $content")
            }
        }

        // 执行请求
        val startTime = System.currentTimeMillis()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            logger.warn("<-- HTTP FAILED: ${e.message}")
            throw e
        }
        val elapsedTime = System.currentTimeMillis() - startTime

        // 记录响应信息
        logger.info("<-- ${response.code} ${request.url} (${elapsedTime}ms)")

        if (logger.isDebugEnabled()) {
            // 记录响应头
            response.headers.forEach { (name, value) ->
                logger.debug("  $name: $value")
            }

            // 记录响应体
            if (response.promisesBody()) {
                val source = response.body?.source()
                source?.request(Long.MAX_VALUE) // Buffer the entire body
                val buffer = source?.buffer

                var charset: Charset = StandardCharsets.UTF_8
                val contentType = response.body?.contentType()
                if (contentType != null) {
                    charset = contentType.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
                }

                if (buffer != null) {
                    val content = buffer.clone().readString(charset)
                    logger.debug("  Body: $content")
                }
            }
        }

        return response
    }
}

