package com.tencent.devops.common.api.pojo.codecc

import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "游标查询数据包装模型")
data class Cursor<out T>(
    @get:Schema(description = "条目限制", required = true)
    val limit: Int,
    @get:Schema(description = "是否有下一页", required = true)
    val hasNext: Boolean,
    @get:Schema(description = "数据", required = true)
    val records: List<T>,
    @get:Schema(description = "下一页的游标", required = true)
    val nextCursor: String
)
