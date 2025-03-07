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
package com.tencent.bk.codecc.defect.llm.log

import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.logging.LogLevel as KLogLevel
import io.ktor.client.plugins.logging.Logger as KLogger

/**
 * Convert Logger to a Ktor's Logger.
 */
internal fun Logger.toKtorLogger() = when (this) {
    Logger.Default -> KLogger.DEFAULT
    Logger.Simple -> KLogger.SIMPLE
    Logger.Empty -> KLogger.EMPTY
}

/**
 * Convert LogLevel to a Ktor's LogLevel.
 */
internal fun LogLevel.toKtorLogLevel() = when (this) {
    LogLevel.All -> KLogLevel.ALL
    LogLevel.Headers -> KLogLevel.HEADERS
    LogLevel.Body -> KLogLevel.BODY
    LogLevel.Info -> KLogLevel.INFO
    LogLevel.None -> KLogLevel.NONE
}
