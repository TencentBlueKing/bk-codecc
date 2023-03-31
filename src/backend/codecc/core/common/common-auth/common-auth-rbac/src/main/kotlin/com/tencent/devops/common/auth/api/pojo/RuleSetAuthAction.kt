package com.tencent.devops.common.auth.api.pojo

enum class RuleSetAuthAction(
    val actionId: String,
    val actionName: String
) {
    CREATE("codecc_rule_set_create", "规则集创建"),
    LIST("codecc_rule_set_list", "规则集列表")
}
