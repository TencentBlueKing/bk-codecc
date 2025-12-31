package com.tencent.bk.codecc.quartz.pojo

import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "节点信息视图")
data class NodeInfoVO(
        @get:Schema(description = "节点序号")
        val nodeNum: Int,
        @get:Schema(description = "服务id")
        val serviceId: String,
        @get:Schema(description = "服务地址")
        val host: String,
        @get:Schema(description = "服务端口")
        val port: Int
)