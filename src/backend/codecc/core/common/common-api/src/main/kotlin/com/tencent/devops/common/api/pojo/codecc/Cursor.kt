package com.tencent.devops.common.api.pojo.codecc

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("游标查询数据包装模型")
data class Cursor<out T>(
    @ApiModelProperty("条目限制", required = true)
    val limit: Int,
    @ApiModelProperty("是否有下一页", required = true)
    val hasNext: Boolean,
    @ApiModelProperty("数据", required = true)
    val records: List<T>,
    @ApiModelProperty("下一页的游标", required = true)
    val nextCursor: String
)
