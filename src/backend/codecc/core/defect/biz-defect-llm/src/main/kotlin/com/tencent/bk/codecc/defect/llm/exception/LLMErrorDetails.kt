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
package com.tencent.bk.codecc.defect.llm.exception

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an error response from the LLM API.
 *
 * @param detail information about the error that occurred.
 */
@Suppress("PLUGIN_IS_NOT_ENABLED")
@Serializable
public data class LLMError(
    @SerialName("error") public val detail: LLMErrorDetails?,
)

/**
 * Represents an error object returned by the LLM API.
 *
 * @param code error code returned by the LLM API.
 * @param message human-readable error message describing the error that occurred.
 * @param param the parameter that caused the error, if applicable.
 * @param type the type of error that occurred.
 */
@Suppress("PLUGIN_IS_NOT_ENABLED")
@Serializable
public data class LLMErrorDetails(
    @SerialName("code") val code: String?,
    @SerialName("message") val message: String?,
    @SerialName("param") val param: String?,
    @SerialName("type") val type: String?,
)
