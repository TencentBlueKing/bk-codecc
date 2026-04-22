package com.codecc.preci.api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 登录认证请求
 *
 * 用于用户登录认证，支持快速登录和手动登录两种方式
 *
 * @property pinToken Pin 和 Token 组合，格式为 "pin:token"
 * @property projectId 蓝盾项目 ID，可选参数，用于关联项目
 *
 * @since 1.0
 */
@Serializable
data class LoginRequest(
    @SerialName("pinToken")
    val pinToken: String,
    
    @SerialName("projectId")
    val projectId: String? = null
)

