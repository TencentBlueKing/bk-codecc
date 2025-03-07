package com.tencent.bk.codecc.defect.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BKAIDevRespBodyVO(
    val result: Boolean,
    val code: String,
    val message: String,
    val data: BKAIDevVO?
)

data class BKAIDevVO(
    @JsonProperty("session_id")
    val sessionId: String,
    val llm: String,
    val result: BKAIDevResult?
)

data class BKAIDevResult(
    val created: String,
    val id: String,
    val model: String,
    val version: String?,
    val choices: List<Choice>
)
