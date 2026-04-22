package com.codecc.preci.api.model.response

import kotlinx.serialization.Serializable

/**
 * 获取当前绑定项目接口响应
 *
 * 对应 PreCI Local Server 的 `GET /auth/project` 接口响应。
 * 返回当前绑定的蓝盾项目 ID。
 *
 * **API 接口：** `GET /auth/project`
 *
 * **响应示例：**
 * ```json
 * {
 *     "projectId": "project_001"
 * }
 * ```
 *
 * @property projectId 当前绑定的项目 ID，如果未绑定则为空字符串
 *
 * @since 1.0
 */
@Serializable
data class GetProjectResponse(
    val projectId: String
)

