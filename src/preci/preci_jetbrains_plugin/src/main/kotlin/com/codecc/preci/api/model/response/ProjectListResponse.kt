package com.codecc.preci.api.model.response

import kotlinx.serialization.Serializable

/**
 * 获取项目列表接口响应
 *
 * 对应 PreCI Local Server 的 `GET /auth/list/projects` 接口响应。
 * 返回当前用户有权限的蓝盾项目列表。
 *
 * **API 接口：** `GET /auth/list/projects`
 *
 * **响应示例：**
 * ```json
 * {
 *     "projects": [
 *         {
 *             "projectId": "project_001",
 *             "projectName": "PreCI 项目"
 *         },
 *         {
 *             "projectId": "project_002",
 *             "projectName": "测试项目"
 *         }
 *     ]
 * }
 * ```
 *
 * @property projects 项目列表
 *
 * @since 1.0
 */
@Serializable
data class ProjectListResponse(
    val projects: List<ProjectInfo>
)

