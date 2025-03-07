package com.tencent.bk.codecc.defect.pojo

data class HunYuanReqBodyVO(
    val model: String,
    val stream: Boolean,
    val messages: List<Chat>
)
