package com.tencent.bk.codecc.quartz.pojo

import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "分片信息视图")
class ShardInfoVO(
        @get:Schema(description = "分片序号")
        val shardNum: Int,
        @get:Schema(description = "分片标记")
        val tag: String,
        @get:Schema(description = "节点清单")
        var nodeList: List<NodeInfoVO>
)