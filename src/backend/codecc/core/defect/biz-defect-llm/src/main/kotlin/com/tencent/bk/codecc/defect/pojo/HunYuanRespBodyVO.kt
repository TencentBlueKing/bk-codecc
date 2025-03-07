package com.tencent.bk.codecc.defect.pojo

data class HunYuanRespBodyVO(
    val id: String,
    val created: Long,
    val usage: Usage,
    val choices: List<Choice>
)
