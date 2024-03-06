package com.tencent.devops.common.auth.api.pojo.external

enum class AuthRoleType(
    val roleName: String,
    val alias: String,
) {
    TASK_OWNER("owner", "拥有者"),
    TASK_EDITOR("editor", "编辑者"),
    TASK_EXECUTOR("executor", "执行者"),
    TASK_VIEWER("viewer", "查看者"),
}
