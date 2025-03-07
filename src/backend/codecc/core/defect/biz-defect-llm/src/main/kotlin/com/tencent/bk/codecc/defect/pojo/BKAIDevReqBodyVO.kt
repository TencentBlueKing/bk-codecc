package com.tencent.bk.codecc.defect.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BKAIDevReqBodyVO(
    @JsonProperty("session_id")
    val sessionId: String,
    @JsonProperty("session_history")
    val sessionHistory: List<Chat>,
    @JsonProperty("chat_prompts")
    var chatPrompts: List<Chat>,
    @JsonProperty("app_collection")
    val appCollection: String,
    val llm: String,
    val stream: Boolean = false,
    @JsonProperty("execute_kwargs")
    val executeKwargs: ExecuteKwargs? = null
)

data class ExecuteKwargs(
    @JsonProperty("stream")
    val stream: Boolean = false,
    @JsonProperty("stream_timeout")
    val streamTimeOut: Int
)
