package com.tencent.bk.codecc.defect.llm.chat

import com.tencent.bk.codecc.defect.constant.LLMConstants
import com.tencent.bk.codecc.defect.llm.extension.streamEventsFrom
import com.tencent.bk.codecc.defect.llm.extension.streamRequestOf
import com.tencent.bk.codecc.defect.llm.http.HttpRequester
import com.tencent.bk.codecc.defect.pojo.serializable.ChatCompletionRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class LLMChatApi(private val requester: HttpRequester) : LLMChat {

    override fun chatCompletions(request: ChatCompletionRequest, authorization: String): Flow<String> {
        val builder = HttpRequestBuilder().apply {
            method = HttpMethod.Patch
            url(
                urlString = "${request.host}${LLMConstants.BKAIDEV_API_URL}${request.sessionId}/"
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
                streamEventsFrom(response)
            }
        }
    }
}
