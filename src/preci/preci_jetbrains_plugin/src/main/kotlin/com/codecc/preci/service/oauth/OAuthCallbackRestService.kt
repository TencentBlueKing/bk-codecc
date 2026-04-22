package com.codecc.preci.service.oauth

import com.codecc.preci.core.log.PreCILogger
import com.intellij.openapi.application.ApplicationManager
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.ide.RestService
import org.jetbrains.io.response
import java.nio.charset.StandardCharsets

/**
 * OAuth 回调 RestService
 *
 * 注册路径 `/api/preci/oauth/callback`，接收 BKAuth 浏览器重定向的 authorization code。
 * 将 code 传递给 [OAuthService]，并向浏览器返回结果 HTML 页面。
 *
 * 浏览器从 BKAuth 重定向到本地回调时，请求中的 Referer/Origin 不是 localhost，
 * 因此覆盖 [isHostTrusted] 始终返回 true。安全性由 OAuth state 参数校验保证。
 *
 * @since 2.0
 */
class OAuthCallbackRestService : RestService() {

    private val logger = PreCILogger.getLogger(OAuthCallbackRestService::class.java)

    override fun getServiceName(): String = "preci/oauth/callback"

    override fun isMethodSupported(method: HttpMethod): Boolean = method === HttpMethod.GET

    /**
     * OAuth 回调来自外部浏览器重定向，跳过 RestService 默认的 host 信任检查。
     * 安全性由 [OAuthService.handleCallback] 中的 state 参数校验保证。
     */
    override fun isHostTrusted(request: FullHttpRequest, urlDecoder: QueryStringDecoder): Boolean = true

    override fun execute(
        urlDecoder: QueryStringDecoder,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ): String? {
        val params = urlDecoder.parameters()

        val error = params["error"]?.firstOrNull()
        if (error != null) {
            val desc = params["error_description"]?.firstOrNull() ?: "Unknown error"
            logger.warn("OAuth callback received error: $error - $desc")
            sendHtmlResponse(request, context, buildResultHtml(success = false, message = "$error: $desc"))
            return null
        }

        val code = params["code"]?.firstOrNull()
        val state = params["state"]?.firstOrNull()

        if (code.isNullOrBlank() || state.isNullOrBlank()) {
            logger.warn("OAuth callback missing code or state. code=${code != null}, state=${state != null}")
            sendHtmlResponse(request, context, buildResultHtml(success = false, message = "缺少必要参数 code 或 state"))
            return null
        }

        val oauthService = try {
            ApplicationManager.getApplication().getService(OAuthService::class.java)
        } catch (e: Exception) {
            logger.error("Failed to get OAuthService", e)
            sendHtmlResponse(request, context, buildResultHtml(success = false, message = "内部错误：无法获取 OAuthService"))
            return null
        }

        val handled = oauthService.handleCallback(code, state)
        if (handled) {
            logger.info("OAuth callback processed successfully for state=$state")
            sendHtmlResponse(request, context, buildResultHtml(success = true, message = "授权成功！请返回 IDE 继续操作。"))
        } else {
            logger.warn("OAuth callback rejected: state mismatch or no active session, state=$state")
            sendHtmlResponse(request, context, buildResultHtml(success = false, message = "授权失败：state 不匹配或无活跃会话"))
        }

        return null
    }

    private fun sendHtmlResponse(request: FullHttpRequest, context: ChannelHandlerContext, html: String) {
        val content = Unpooled.copiedBuffer(html, StandardCharsets.UTF_8)
        val httpResponse = response("text/html; charset=utf-8", content)
        sendResponse(request, context, httpResponse)
    }

    @Suppress("MaxLineLength")
    private fun buildResultHtml(success: Boolean, message: String): String {
        val color = if (success) "#52c41a" else "#ff4d4f"
        val icon = if (success) "&#10003;" else "&#10007;"
        val title = if (success) "PreCI 授权成功" else "PreCI 授权失败"
        return """
            |<!DOCTYPE html>
            |<html lang="zh-CN">
            |<head><meta charset="utf-8"><title>$title</title>
            |<style>
            |  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            |         display: flex; justify-content: center; align-items: center; min-height: 100vh;
            |         margin: 0; background: #f5f5f5; }
            |  .card { background: #fff; border-radius: 8px; padding: 48px; text-align: center;
            |          box-shadow: 0 2px 8px rgba(0,0,0,0.1); max-width: 400px; }
            |  .icon { font-size: 48px; color: $color; }
            |  .msg  { margin-top: 16px; color: #333; font-size: 16px; }
            |  .hint { margin-top: 12px; color: #999; font-size: 13px; }
            |</style></head>
            |<body><div class="card">
            |  <div class="icon">$icon</div>
            |  <div class="msg">$message</div>
            |  <div class="hint">此窗口可以安全关闭</div>
            |</div></body></html>
        """.trimMargin()
    }
}
