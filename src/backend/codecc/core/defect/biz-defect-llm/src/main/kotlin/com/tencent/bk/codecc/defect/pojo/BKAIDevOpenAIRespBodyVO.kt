package com.tencent.bk.codecc.defect.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BKAIDevOpenAIRespBodyVO(
    val result: Boolean,
    val data: List<OpenAIVO>?,
    val code: String,
    val message: String
)

data class OpenAIVO(
    @JsonProperty("service_time")
    val serviceTime: Double,
    @JsonProperty("full_time")
    val fullTime: String,
    val result: BKAIDevResult?
)
