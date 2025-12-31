/*
 * MIT License
 *
 * Copyright (c) 2021 Mouaad Aallam
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.bk.codecc.defect.llm.http

import com.tencent.bk.codecc.defect.llm.exception.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** HTTP transport layer */
class HttpTransport(private val httpClient: HttpClient) : HttpRequester {

    /** Perform an HTTP request and get a result */
    override suspend fun <T : Any> perform(info: TypeInfo, block: suspend (HttpClient) -> HttpResponse): T {
        try {
            val response = block(httpClient)
            return response.body(info)
        } catch (e: Exception) {
            throw handleException(e)
        }
    }

    override suspend fun <T : Any> perform(
        builder: HttpRequestBuilder,
        block: suspend (response: HttpResponse) -> T
    ) {
        try {
            HttpStatement(builder = builder, client = httpClient).execute(block)
        } catch (e: Exception) {
            throw handleException(e)
        }
    }

    override fun close() {
        httpClient.close()
    }

    /**
     * Handles various exceptions that can occur during an API request and converts them into appropriate
     * [LLMException] instances.
     */
    private suspend fun handleException(e: Throwable) = when (e) {
        is CancellationException -> e // propagate coroutine cancellation
        is ClientRequestException -> apiException(e)
        is ServerResponseException -> LLMServerException(e)
        is HttpRequestTimeoutException, is SocketTimeoutException, is ConnectTimeoutException -> LLMTimeoutException(e)
        is IOException -> GenericIOException(e)
        else -> LLMHttpException(e)
    }

    /**
     * Converts a [ClientRequestException] into a corresponding [LLMAPIException] based on the HTTP status code.
     * This function helps in handling specific API errors and categorizing them into appropriate exception classes.
     */
    private suspend fun apiException(exception: ClientRequestException): Exception {
        val response = exception.response
        val status = response.status.value

        // 获取原始响应内容
        val rawResponse = try {
            response.bodyAsText()
        } catch (e: Exception) {
            "Failed to read response body: ${e.message}"
        }

        // 尝试直接解析为LLMError格式，兼容原有接口
        val llmError = try {
            response.body<LLMError>()
        } catch (e: Exception) {
            null
        }

        // 尝试解析为json对象, 模型方存在多种异常响应格式
        val customError = try {
            Json.parseToJsonElement(rawResponse).jsonObject
        } catch (e: Exception) {
            null
        }

        // 根据响应格式创建错误对象
        val error = when {
            llmError != null -> llmError
            customError != null -> {
                LLMError(
                    detail = LLMErrorDetails(
                        code = customError["code"]?.jsonPrimitive?.contentOrNull ?: "UNKNOWN",
                        message = customError["message"]?.jsonPrimitive?.contentOrNull ?: "unknown error",
                        param = customError["param"]?.jsonPrimitive?.contentOrNull,
                        type = customError["type"]?.jsonPrimitive?.contentOrNull ?: "custom_llm_error"
                    )
                )
            }
            else -> {
                LLMError(
                    detail = LLMErrorDetails(
                        code = "DESERIALIZATION_FAILED",
                        message = "failed to parse error response, raw response: $rawResponse",
                        param = null,
                        type = "internal_error"
                    )
                )
            }
        }

        return when (status) {
            429 -> RateLimitException(status, error, exception)
            400, 404, 415 -> InvalidRequestException(status, error, exception)
            401 -> AuthenticationException(status, error, exception)
            403 -> PermissionException(status, error, exception)
            else -> UnknownAPIException(status, error, exception)
        }
    }
}
