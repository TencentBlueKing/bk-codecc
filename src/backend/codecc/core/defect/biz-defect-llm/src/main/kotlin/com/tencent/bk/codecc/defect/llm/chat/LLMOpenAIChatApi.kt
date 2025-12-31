package com.tencent.bk.codecc.defect.llm.chat

import com.tencent.bk.codecc.defect.llm.extension.streamEventsFromForOpenAI
import com.tencent.bk.codecc.defect.llm.extension.streamRequestOf
import com.tencent.bk.codecc.defect.llm.http.HttpRequester
import com.tencent.bk.codecc.defect.pojo.serializable.BkChatCompletionChunk
import com.tencent.bk.codecc.defect.pojo.serializable.ChatCompletionRequestV2
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class LLMOpenAIChatApi(
    private val requester: HttpRequester,
    private val apiPath: String
) {
    fun chatCompletions(
        request: ChatCompletionRequestV2,
        authorization: String
    ): Flow<BkChatCompletionChunk> {
        val builder = HttpRequestBuilder().apply {
            method = HttpMethod.Post
            url(
                path = apiPath
            )
            setBody(streamRequestOf(request))
            contentType(ContentType.Application.Json)
            accept(ContentType.Text.EventStream)
            headers {
                append(HttpHeaders.CacheControl, "no-cache")
                append(HttpHeaders.Connection, "keep-alive")
                append("X-Bkapi-Authorization", authorization)
            }
        }
        return flow {
            requester.perform(builder) { response ->
                streamEventsFromForOpenAI(response)
            }
        }
    }
}
