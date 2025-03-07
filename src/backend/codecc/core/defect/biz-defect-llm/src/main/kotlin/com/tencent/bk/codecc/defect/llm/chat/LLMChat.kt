package com.tencent.bk.codecc.defect.llm.chat

import com.tencent.bk.codecc.defect.pojo.serializable.ChatCompletionRequest
import kotlinx.coroutines.flow.Flow

/**
 * Given a chat conversation, the model will return a chat completion response.
 */
public interface LLMChat {

    /**
     * Stream variant of [ChatCompletions].
     */
    public fun chatCompletions(request: ChatCompletionRequest, authorization: String): Flow<String>
}
