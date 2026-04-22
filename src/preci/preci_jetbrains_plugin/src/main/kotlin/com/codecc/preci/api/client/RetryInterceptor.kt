package com.codecc.preci.api.client

import com.codecc.preci.core.log.PreCILogger
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * HTTP 请求重试拦截器
 *
 * 当网络请求失败时自动重试，提高请求的可靠性。
 * 仅对网络异常（IOException）进行重试，业务异常（HTTP 4xx/5xx）不重试。
 *
 * **重试策略：**
 * - 重试次数：默认 3 次
 * - 重试间隔：递增延迟（第 1 次 500ms，第 2 次 1000ms，第 3 次 1500ms）
 * - 重试条件：仅网络异常（连接失败、超时等）
 *
 * @property maxRetries 最大重试次数
 *
 * @since 1.0
 */
class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    private val logger = PreCILogger.getLogger(RetryInterceptor::class.java)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0
        var lastException: IOException? = null

        while (attempt <= maxRetries) {
            try {
                if (attempt > 0) {
                    val delayMs = if (lastException is ConnectException || isConnectionError(lastException)) {
                        2000L * attempt
                    } else {
                        500L * attempt
                    }
                    logger.info("Retrying request (attempt $attempt/$maxRetries) after ${delayMs}ms delay: ${request.url}")
                    Thread.sleep(delayMs)
                }

                val response = chain.proceed(request)

                // 请求成功，返回响应
                if (attempt > 0) {
                    logger.info("Request succeeded after $attempt retry(ies): ${request.url}")
                }
                return response

            } catch (e: IOException) {
                if (isReadTimeout(e)) {
                    logger.warn("Read timed out, not retrying (server is reachable but busy): ${request.url}")
                    throw e
                }

                lastException = e
                attempt++

                if (attempt > maxRetries) {
                    logger.warn("Request failed after $maxRetries retry(ies): ${request.url}", e)
                    throw e
                }

                logger.debug("Request failed (attempt $attempt/$maxRetries): ${request.url} - ${e.message}")
            }
        }

        // 理论上不会到达这里，但为了类型安全
        throw lastException ?: IOException("Request failed after retries")
    }

    private fun isReadTimeout(e: IOException): Boolean {
        return e is SocketTimeoutException && e.message?.lowercase()?.contains("read") == true
    }

    private fun isConnectionError(e: IOException?): Boolean {
        if (e == null) return false
        if (e is ConnectException) return true
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("connection refused") || msg.contains("failed to connect")
    }
}

