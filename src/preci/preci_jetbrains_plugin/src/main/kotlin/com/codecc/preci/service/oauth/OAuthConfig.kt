package com.codecc.preci.service.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OAuth 配置
 *
 * 从 classpath 中的 config.json 加载，包含 BKAuth OAuth 所需的所有参数。
 *
 * @property bkauthBaseUrl BKAuth 服务基础 URL
 * @property clientId 客户端 ID
 * @property resource 资源标识
 * @property scope 权限范围
 * @property redirectPath 回调路径（不含 host 和端口）
 *
 * @since 2.0
 */
@Serializable
data class OAuthConfig(
    @SerialName("bkauthBaseUrl")
    val bkauthBaseUrl: String,

    @SerialName("clientId")
    val clientId: String,

    @SerialName("resource")
    val resource: String,

    @SerialName("scope")
    val scope: String,

    @SerialName("redirectPath")
    val redirectPath: String
)
