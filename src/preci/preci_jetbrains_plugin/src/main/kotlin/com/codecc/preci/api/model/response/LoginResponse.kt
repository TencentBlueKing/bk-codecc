package com.codecc.preci.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 登录认证响应
 *
 * @property projectId 登录成功后关联的项目 ID
 * @property userId 登录成功的用户 ID（RTX 名）
 *
 * @since 1.0
 */
@Serializable
data class LoginResponse(
    @SerialName("projectId")
    val projectId: String,
    
    @SerialName("userId")
    val userId: String
)

