package com.codecc.preci.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * BKAuth OAuth token 响应
 *
 * 对应 BKAuth POST /oauth2/token 的成功响应。
 *
 * @property accessToken 访问令牌
 * @property tokenType 令牌类型（通常为 "Bearer"）
 * @property expiresIn 有效期（秒）
 * @property refreshToken 刷新令牌
 * @property scope 授权范围
 *
 * @since 2.0
 */
@Serializable
data class OAuthTokenResponse(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("token_type")
    val tokenType: String = "Bearer",

    @SerialName("expires_in")
    val expiresIn: Long,

    @SerialName("refresh_token")
    val refreshToken: String,

    @SerialName("scope")
    val scope: String = ""
)
