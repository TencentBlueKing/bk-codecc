package com.tencent.bk.codecc.defect.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import lombok.Data

@Data
@ApiModel("修复建议评价")
data class DefectSuggestionEvaluateVO(

    @ApiModelProperty(value = "defectId", required = true)
    var defectId: String,

    @ApiModelProperty(value = "好评价", required = true)
    var goodEvaluates: MutableList<String>,

    @ApiModelProperty(value = "坏评价", required = true)
    var badEvaluates: MutableList<String>
)
