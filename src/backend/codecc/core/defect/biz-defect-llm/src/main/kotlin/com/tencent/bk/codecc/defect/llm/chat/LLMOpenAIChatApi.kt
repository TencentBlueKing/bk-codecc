package com.tencent.bk.codecc.defect.llm.chat

import com.tencent.bk.codecc.defect.constant.LLMConstants
import com.tencent.bk.codecc.defect.llm.extension.streamEventsFromForOpenAI
import com.tencent.bk.codecc.defect.llm.extension.streamRequestOf
import com.tencent.bk.codecc.defect.llm.http.HttpRequester
import com.tencent.bk.codecc.defect.pojo.serializable.BkChatCompletionChunk
import com.tencent.bk.codecc.defect.pojo.serializable.ChatCompletionRequestV2
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory

internal class LLMOpenAIChatApi(private val requester: HttpRequester) {
    private val logger = LoggerFactory.getLogger(LLMOpenAIChatApi::class.java)

    fun chatCompletions(
        request: ChatCompletionRequestV2,
        authorization: String
    ): Flow<BkChatCompletionChunk> {
        val builder = HttpRequestBuilder().apply {
            method = HttpMethod.Post
            url(
                path = LLMConstants.LLM_GATEWAY_URL_USERSPACE
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