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


/**
 * Represents an exception thrown when an error occurs while interacting with the LLM API.
 *
 * @property statusCode the HTTP status code associated with the error.
 * @property error an instance of [LLMError] containing information about the error that occurred.
 */
public sealed class LLMAPIException(
    public val statusCode: Int,
    public val error: LLMError,
    throwable: Throwable? = null,
) : LLMException(message = error.detail?.message, throwable = throwable)

/**
 * Represents an exception thrown when the LLM API rate limit is exceeded.
 */
public class RateLimitException(
    statusCode: Int,
    error: LLMError,
    throwable: Throwable? = null
) : LLMAPIException(statusCode, error, throwable)

/**
 * Represents an exception thrown when an invalid request is made to the LLM API.
 */
public class InvalidRequestException(
    statusCode: Int,
    error: LLMError,
    throwable: Throwable? = null
) : LLMAPIException(statusCode, error, throwable)

/**
 * Represents an exception thrown when an authentication error occurs while interacting with the LLM API.
 */
public class AuthenticationException(
    statusCode: Int,
    error: LLMError,
    throwable: Throwable? = null
) : LLMAPIException(statusCode, error, throwable)

/**
 * Represents an exception thrown when a permission error occurs while interacting with the LLM API.
 */
public class PermissionException(
    statusCode: Int,
    error: LLMError,
    throwable: Throwable? = null
) : LLMAPIException(statusCode, error, throwable)

/**
 * Represents an exception thrown when an unknown error occurs while interacting with the LLM API.
 * This exception is used when the specific type of error is not covered by the existing subclasses.
 */
public class UnknownAPIException(
    statusCode: Int,
    error: LLMError,
    throwable: Throwable? = null
) : LLMAPIException(statusCode, error, throwable)
