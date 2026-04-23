package com.codecc.preci.service.oauth

import com.codecc.preci.api.model.response.OAuthTokenResponse
import com.codecc.preci.core.log.PreCILogger
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * OAuth 流程中的异常
 *
 * @since 2.0
 */
class OAuthException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * BKAuth HTTP 客户端
 *
 * 负责与 BKAuth 服务端交互：构造授权 URL、用 authorization code 换取 token。
 * 使用独立的 OkHttpClient（不走 PreCIApiClient，因为直连 BKAuth 而非 Local Server）。
 *
 * @since 2.0
 */
object BKAuthClient {

    private val logger = PreCILogger.getLogger(BKAuthClient::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * 构造 BKAuth 授权 URL
     *
     * @param config OAuth 配置
     * @param codeChallenge PKCE code_challenge
     * @param state 防 CSRF 随机字符串
     * @param redirectUri 完整回调地址（含 host 和端口）
     * @return 完整的授权 URL
     */
    fun buildAuthorizeUrl(
        config: OAuthConfig,
        codeChallenge: String,
        state: String,
        redirectUri: String
    ): String {
        val params = buildList {
            add("response_type" to "code")
            add("client_id" to config.clientId)
            add("redirect_uri" to redirectUri)
            add("code_challenge" to codeChallenge)
            add("code_challenge_method" to "S256")
            add("state" to state)
            add("resource" to config.resource)
            if (config.scope.isNotBlank()) {
                add("scope" to config.scope)
            }
        }

        val queryString = params.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }

        return "${config.bkauthBaseUrl}/oauth2/authorize?$queryString"
    }

    /**
     * 用 authorization code 换取 token
     *
     * POST {bkauthBaseUrl}/oauth2/token，使用 application/x-www-form-urlencoded 格式。
     *
     * @param config OAuth 配置
     * @param code 授权码
     * @param codeVerifier PKCE code_verifier
     * @param redirectUri 回调地址（须与授权请求一致）
     * @return token 响应
     * @throws OAuthException 换取失败
     */
    fun exchangeCodeForToken(
        config: OAuthConfig,
        code: String,
        codeVerifier: String,
        redirectUri: String
    ): OAuthTokenResponse {
        val formBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("client_id", config.clientId)
            .add("redirect_uri", redirectUri)
            .add("code_verifier", codeVerifier)
            .build()

        val request = Request.Builder()
            .url("${config.bkauthBaseUrl}/oauth2/token")
            .post(formBody)
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                logger.error("Token exchange failed: HTTP ${response.code}, body=$body")
                throw OAuthException("Token exchange failed (HTTP ${response.code}): $body")
            }

            return json.decodeFromString<OAuthTokenResponse>(body)
        } catch (e: OAuthException) {
            throw e
        } catch (e: IOException) {
            logger.error("Token exchange network error", e)
            throw OAuthException("Network error during token exchange: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Token exchange unexpected error", e)
            throw OAuthException("Unexpected error during token exchange: ${e.message}", e)
        }
    }
}
