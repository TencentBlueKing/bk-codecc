package com.tencent.devops.common.auth.api.pojo.external

enum class CodeCCAuthAction(val actionName: String,
                            val alias: String) {
    // 新增的action，用于兼容映射
    CREATE("codecc_task_create", "新建任务"),
    LIST("codecc_task_list", "任务列表"),
    SETTING("codecc_task_setting", "任务设置"),
    RULESET_CREATE("codecc_ruleset_create", "规则集创建"),
    RULESET_LIST("codecc_ruleset_list", "规则集列表"),
    IGNORE_TYPE_MANAGE("codecc_ignore_type_manage", "忽略配置"),

    TASK_MANAGE("task_manage", "任务设置"),
    ANALYZE("analyze", "执行分析"),
    DEFECT_MANAGE("defect_manage", "管理告警"),
    DEFECT_VIEW("defect_view", "查看告警"),
    REPORT_VIEW("report_view", "查看报表");
}