package com.tencent.bk.codecc.defect.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class ChatCompletionResponse(
    @JsonProperty("id")
    val id: String? = null,

    @JsonProperty("object")
    val objectName: String? = null,

    @JsonProperty("created")
    val created: Long? = null,

    @JsonProperty("model")
    val model: String? = null,

    @JsonProperty("choices")
    val choices: List<Choice>? = null,

    @JsonProperty("usage")
    val usage: Usage? = null
) {
    data class Choice(
        @JsonProperty("index")
        val index: Int? = null,

        // 同时支持流式和非流式响应
        @JsonProperty("message")
        val message: Message? = null,

        @JsonProperty("finish_reason")
        val finishReason: String? = null
    )

    // 流式响应专用数据结构
    data class Delta(
        @JsonProperty("role")
        val role: String? = null,

        @JsonProperty("content")
        val content: String? = null
    )

    data class Message(
        @JsonProperty("role")
        val role: String? = null,

        @JsonProperty("content")
        val content: String? = null,

        @JsonProperty("tool_calls")
        val toolCalls: Any? = null,

        @JsonProperty("function_call")
        val functionCall: Map<String, Any>? = null,

        @JsonProperty("reasoning_content")
        val reasoningContent: String? = null
    )

    data class Usage(
        @JsonProperty("prompt_tokens")
        val promptTokens: Int? = null,

        @JsonProperty("completion_tokens")
        val completionTokens: Int? = null,

        @JsonProperty("total_tokens")
        val totalTokens: Int? = null
    )
}
