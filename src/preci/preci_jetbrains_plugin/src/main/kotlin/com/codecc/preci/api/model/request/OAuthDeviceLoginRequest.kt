package com.codecc.preci.api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OAuth Device Login 请求
 *
 * 对应 Local Server 的 POST /auth/oauth/device/login 接口。
 * 将插件获取的 OAuth token 交给 Local Server 管理。
 *
 * @property accessToken 访问令牌
 * @property refreshToken 刷新令牌
 * @property expiresIn 有效期（秒）
 * @property projectId 蓝盾项目 ID（可选）
 *
 * @since 2.0
 */
@Serializable
data class OAuthDeviceLoginRequest(
    @SerialName("accessToken")
    val accessToken: String,

    @SerialName("refreshToken")
    val refreshToken: String,

    @SerialName("expiresIn")
    val expiresIn: Long,

    @SerialName("projectId")
    val projectId: String? = null
)
