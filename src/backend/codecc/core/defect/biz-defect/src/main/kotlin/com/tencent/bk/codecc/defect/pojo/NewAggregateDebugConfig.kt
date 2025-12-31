package com.tencent.bk.codecc.defect.pojo

import com.tencent.devops.common.constant.ComConstants

class NewAggregateDebugConfig(
    var rules: List<NewAggregateMatchRule> = emptyList(),
    var taskIds: List<Long> = emptyList(),
)

class NewAggregateMatchRule(
    var toolName: String = ComConstants.EMPTY_STRING,
    var checkers: List<String> = emptyList(),
)
