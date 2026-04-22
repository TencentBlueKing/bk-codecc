package com.codecc.preci.service.oauth

import com.codecc.preci.core.log.PreCILogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * OAuth 流程编排服务
 *
 * 管理 OAuth 会话状态，协调 PKCE 生成、浏览器授权、回调处理和 token 交换。
 * 同一时刻只允许一个活跃的 OAuth 会话。
 *
 * 使用方式：
 * 1. [createSession] 创建会话，获得 state
 * 2. 用 state + codeVerifier 构造授权 URL 并打开浏览器
 * 3. [awaitAuthorizationCode] 挂起等待回调
 * 4. [handleCallback] 由 RestService 调用，传入 code 和 state
 * 5. 等待结束后用 code + codeVerifier 换 token
 *
 * @since 2.0
 */
class OAuthService {

    private val logger = PreCILogger.getLogger(OAuthService::class.java)

    private data class OAuthSession(
        val state: String,
        val codeVerifier: String,
        val codeDeferred: CompletableDeferred<String>
    )

    @Volatile
    private var activeSession: OAuthSession? = null

    private val lock = Any()

    /**
     * 创建新的 OAuth 会话
     *
     * 如果已有活跃会话，先取消旧会话再创建新会话。
     *
     * @return 本次会话的 state 参数
     */
    fun createSession(): String {
        synchronized(lock) {
            activeSession?.let {
                logger.info("Cancelling existing OAuth session, state=${it.state}")
                it.codeDeferred.cancel()
            }

            val state = PKCEHelper.generateState()
            val codeVerifier = PKCEHelper.generateCodeVerifier()
            activeSession = OAuthSession(
                state = state,
                codeVerifier = codeVerifier,
                codeDeferred = CompletableDeferred()
            )
            logger.info("Created new OAuth session, state=$state")
            return state
        }
    }

    /**
     * 获取当前会话的 code_verifier
     *
     * @param state 会话 state
     * @return code_verifier，如果 state 不匹配或无会话返回 null
     */
    fun getSessionCodeVerifier(state: String): String? {
        synchronized(lock) {
            val session = activeSession ?: return null
            return if (session.state == state) session.codeVerifier else null
        }
    }

    /**
     * 挂起等待 authorization code
     *
     * @param state 会话 state（用于校验一致性）
     * @param timeoutMs 超时时间（毫秒）
     * @return authorization code
     * @throws OAuthException 如果超时
     * @throws kotlinx.coroutines.CancellationException 如果会话被取消
     */
    suspend fun awaitAuthorizationCode(state: String, timeoutMs: Long): String {
        val session = synchronized(lock) {
            activeSession?.takeIf { it.state == state }
        } ?: throw OAuthException("No active OAuth session for state=$state")

        val code = withTimeoutOrNull(timeoutMs) { session.codeDeferred.await() }
            ?: throw OAuthException("OAuth authorization timed out after ${timeoutMs / 1000} seconds")

        return code
    }

    /**
     * 处理浏览器回调
     *
     * 由 [OAuthCallbackRestService] 在非协程上下文中调用。
     * 仅验证 state 并传递 code，不做任何 HTTP 请求。
     *
     * @param code 授权码
     * @param state 回调中的 state 参数
     * @return true 如果 state 匹配且成功传递 code；false 如果 state 不匹配或无活跃会话
     */
    fun handleCallback(code: String, state: String): Boolean {
        synchronized(lock) {
            val session = activeSession ?: run {
                logger.warn("OAuth callback received but no active session")
                return false
            }

            if (session.state != state) {
                logger.warn("OAuth callback state mismatch: expected=${session.state}, got=$state")
                return false
            }

            session.codeDeferred.complete(code)
            logger.info("OAuth callback received, code delivered to waiting coroutine")
            return true
        }
    }

    /**
     * 取消当前活跃会话
     */
    fun cancelSession() {
        synchronized(lock) {
            activeSession?.let {
                it.codeDeferred.cancel()
                logger.info("OAuth session cancelled, state=${it.state}")
            }
            activeSession = null
        }
    }
}
