package com.codecc.preci.api.model.response

import kotlinx.serialization.Serializable

/**
 * 蓝盾项目信息
 *
 * 表示一个蓝盾项目的基本信息，包括项目 ID 和项目名称。
 *
 * **数据来源：**
 * - `GET /auth/list/projects` 接口响应
 *
 * **使用场景：**
 * - 在设置页面展示可用的项目列表
 * - 在项目选择对话框中展示项目信息
 *
 * @property projectId 项目唯一标识 ID
 * @property projectName 项目显示名称
 *
 * @since 1.0
 */
@Serializable
data class ProjectInfo(
    val projectId: String,
    val projectName: String
)

