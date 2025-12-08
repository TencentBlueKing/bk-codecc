package com.tencent.bk.codecc.defect.vo

import io.swagger.v3.oas.annotations.media.Schema
import lombok.Data

@Data
@Schema(description = "修复建议评价")
data class DefectSuggestionEvaluateVO(

    @get:Schema(description = "defectId", required = true)
    var defectId: String,

    @get:Schema(description = "好评价", required = true)
    var goodEvaluates: MutableList<String>,

    @get:Schema(description = "坏评价", required = true)
    var badEvaluates: MutableList<String>
)
