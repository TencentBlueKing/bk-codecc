package com.tencent.devops.common.auth.api.pojo.external

/**
 * 平台RBAC模型资源类型
 */
enum class ResourceType(
    val id: String
) {
    TASK("codecc_task"),
    PIPELINE("pipeline"),
    PROJECT("project")
}
