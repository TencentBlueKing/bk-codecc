package com.codecc.preci.api.client

import com.codecc.preci.api.model.response.ErrorResponse
import com.codecc.preci.core.http.PreCIErrorCode
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.auth.AuthService
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * 认证错误处理拦截器
 *
 * 在 HTTP 响应层面检测认证错误（错误码 100005），通过 IDE 通知提示用户重新授权。
 *
 * **工作流程：**
 * 1. 拦截 HTTP 响应
 * 2. 检测是否返回错误码 100005（Access Token 无效）
 * 3. 如果是，显示带"重新授权"按钮的 IDE 通知
 * 4. 用户点击按钮后触发 OAuth 浏览器授权流程
 * 5. 返回原始错误响应（不自动重试）
 *
 * **防止重复提示：**
 * - 登录 / OAuth 请求本身不会触发处理（检查 URL 路径）
 * - 使用全局标记避免并发重复弹窗
 *
 * @since 1.0
 */
class AuthRetryInterceptor : Interceptor {
    private val logger = PreCILogger.getLogger(AuthRetryInterceptor::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    companion object {
        /**
         * 自动登录的最小间隔时间（毫秒）
         * 避免短时间内重复触发登录
         */
        private const val AUTO_LOGIN_INTERVAL_MS = 30_000L  // 30秒

        /**
         * 全局标记：是否正在自动登录（防止多个 PreCIApiClient 实例并发触发）
         *
         * 使用 companion object 确保所有 AuthRetryInterceptor 实例共享同一标志。
         */
        @Volatile
        @JvmStatic
        private var isAutoLoginInProgress = false

        /**
         * 全局记录上次自动登录的时间（毫秒），所有实例共享。
         */
        @Volatile
        @JvmStatic
        private var lastAutoLoginTime = 0L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        if (response.isSuccessful) {
            return response
        }

        if (shouldHandleAuthError(response, originalRequest)) {
            logger.warn("检测到 Access Token 无效错误（100005），提示用户重新授权")

            synchronized(AuthRetryInterceptor::class.java) {
                if (isAutoLoginInProgress) {
                    logger.info("重新授权已在进行中，跳过")
                    return response
                }
                isAutoLoginInProgress = true
                lastAutoLoginTime = System.currentTimeMillis()
            }

            showReauthorizeNotification()
        }

        return response
    }

    private fun showReauthorizeNotification() {
        try {
            val project = ProjectManager.getInstance().defaultProject
            val notification = NotificationGroupManager.getInstance()
                .getNotificationGroup("PreCI.Auth")
                .createNotification(
                    "认证已过期",
                    "您的访问令牌已过期，请重新授权以继续使用。",
                    NotificationType.WARNING
                )

            notification.addAction(
                NotificationAction.createSimple("重新授权") {
                    notification.expire()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val authService = AuthService.getInstance()
                            authService.loginWithOAuth()
                        } catch (e: Exception) {
                            logger.error("重新授权失败", e)
                        } finally {
                            isAutoLoginInProgress = false
                        }
                    }
                }
            )

            Notifications.Bus.notify(notification, project)
        } catch (e: Exception) {
            logger.warn("无法显示重新授权通知", e)
            isAutoLoginInProgress = false
        }
    }

    /**
     * 判断是否应该处理认证错误
     *
     * @param response HTTP 响应
     * @param request HTTP 请求
     * @return true 如果应该处理，false 否则
     */
    private fun shouldHandleAuthError(response: Response, request: Request): Boolean {
        // 如果正在自动登录，避免并发
        if (isAutoLoginInProgress) {
            logger.debug("正在自动登录中，跳过处理")
            return false
        }

        // 检查距离上次自动登录的时间间隔
        val timeSinceLastLogin = System.currentTimeMillis() - lastAutoLoginTime
        if (lastAutoLoginTime > 0 && timeSinceLastLogin < AUTO_LOGIN_INTERVAL_MS) {
            logger.debug("距离上次自动登录仅 ${timeSinceLastLogin}ms，未超过最小间隔 ${AUTO_LOGIN_INTERVAL_MS}ms，跳过处理")
            return false
        }

        // 如果请求本身就是登录或 OAuth 相关请求，不处理
        if (request.url.encodedPath.contains("/auth/login") ||
            request.url.encodedPath.contains("/auth/oauth")) {
            return false
        }

        // 检查响应是否包含错误码 100005
        return try {
            val responseBody = response.peekBody(Long.MAX_VALUE).string()
            if (responseBody.isNotEmpty()) {
                val errorResponse = json.decodeFromString<ErrorResponse>(responseBody)
                val errorCode = errorResponse.extractErrorCode()
                errorCode == PreCIErrorCode.CODE_INVALID_ACCESS_TOKEN
            } else {
                false
            }
        } catch (e: Exception) {
            logger.debug("无法解析响应体以检查错误码: ${e.message}")
            false
        }
    }
}
