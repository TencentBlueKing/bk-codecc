package com.tencent.devops.common.auth.api.pojo

enum class TaskAuthAction(
    val actionId: String,
    val actionName: String
) {
    ANALYZE("codecc_task_analyze", "分析任务"),
    MANAGE_DEFECT("codecc_task_manage-defect", "问题管理"),
    VIEW_DEFECT("codecc_task_view-defect", "查看问题列表和详情"),
    VIEW_REPORT("codecc_task_view-report", "查看报表"),
    CREATE("codecc_task_create", "新建任务"),
    LIST("codecc_task_list", "任务列表"),
    SETTING("codecc_task_setting", "任务设置"),
    MANAGE("codecc_task_manage", "任务权限管理")
}
