@file:Suppress("DEPRECATION")

package com.tencent.bk.codecc.defect.pojo.serializable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Creates a completion for the chat message.
 */
@Suppress("PLUGIN_IS_NOT_ENABLED")
@Serializable
class ChatCompletionRequest(
    @SerialName("host") var host: String? = null,
    @SerialName("session_id") var sessionId: String? = null,
    @SerialName("session_history") var sessionHistory: List<SerializableChat>? = null,
    @SerialName("chat_prompts") val chatPrompts: List<SerializableChat>,
    @SerialName("app_collection") var appCollection: String? = null,
    @SerialName("llm") var llm: String? = null,
    @SerialName("stream") var stream: Boolean = false,
    @SerialName("execute_kwargs") var executeKwargs: SerializableExecuteKwargs? = null,
)

/**
 * The messages to generate chat completions for.
 */
fun chatCompletionRequest(block: ChatCompletionRequestBuilder.() -> Unit): ChatCompletionRequest =
    ChatCompletionRequestBuilder().apply(block).build()

/**
 * Creates a completion for the chat message.
 */
@LLMDsl
class ChatCompletionRequestBuilder {
    var host: String? = null
    var sessionId: String? = null
    var sessionHistory: List<SerializableChat>? = null
    var chatPrompts: List<SerializableChat>? = null
    var appCollection: String? = null
    var llm: String? = null
    var stream: Boolean = false
    var executeKwargs: SerializableExecuteKwargs? = null

    fun chatPrompts(block: ChatMessagesBuilder.() -> Unit) {
        chatPrompts = ChatMessagesBuilder().apply(block).chatPrompts
    }


    /**
     * Builder of [ChatCompletionRequest] instances.
     */
    fun build(): ChatCompletionRequest = ChatCompletionRequest(
        host = host,
        sessionId = sessionId,
        chatPrompts = requireNotNull(chatPrompts) { "chatPrompts is required" },
        stream = stream,
        sessionHistory = sessionHistory,
        executeKwargs = executeKwargs,
        appCollection = appCollection,
        llm = llm
    )
}

/**
 * Creates a list of [ChatMessage].
 */
@LLMDsl
class ChatMessagesBuilder {
    internal val chatPrompts = mutableListOf<SerializableChat>()

    fun chatPrompt(block: SerializableChatBuilder.() -> Unit) {
        chatPrompts += SerializableChatBuilder().apply(block).build()
    }

}
