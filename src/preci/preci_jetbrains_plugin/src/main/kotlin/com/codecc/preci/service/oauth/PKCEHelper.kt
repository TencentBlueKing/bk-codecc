package com.codecc.preci.service.oauth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * PKCE (Proof Key for Code Exchange) 参数生成工具
 *
 * 根据 RFC 7636 标准生成 OAuth 2.0 PKCE 所需的参数。
 *
 * @since 2.0
 */
object PKCEHelper {

    private val secureRandom = SecureRandom()

    /**
     * 生成 code_verifier
     *
     * 使用 SecureRandom 生成 32 字节随机数，base64url 编码后长度为 43 字符。
     *
     * @return base64url 编码的随机字符串（43 字符）
     */
    fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * 根据 code_verifier 生成 code_challenge
     *
     * code_challenge = BASE64URL(SHA256(code_verifier))
     *
     * @param codeVerifier 由 [generateCodeVerifier] 生成的验证码
     * @return base64url 编码的 SHA-256 哈希值
     */
    fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /**
     * 生成防 CSRF 的随机 state 参数
     *
     * @return base64url 编码的 16 字节随机字符串
     */
    fun generateState(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
