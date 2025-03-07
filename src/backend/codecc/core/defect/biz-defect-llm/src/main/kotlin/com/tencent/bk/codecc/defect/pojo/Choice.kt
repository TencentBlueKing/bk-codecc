package com.tencent.bk.codecc.defect.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class Choice(
    @JsonProperty("finish_reason")
    val finishReason: String,
    val message: Chat
)
