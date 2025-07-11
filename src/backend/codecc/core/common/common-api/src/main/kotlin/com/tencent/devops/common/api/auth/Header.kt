/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.auth


const val AUTH_HEADER_DEVOPS_USER_ID = "X-DEVOPS-UID"
const val AUTH_HEADER_DEVOPS_PROJECT_ID: String = "X-DEVOPS-PROJECT-ID"
const val AUTH_HEADER_DEVOPS_ACCESS_TOKEN: String = "X-DEVOPS-ACCESS-TOKEN"
const val AUTH_HEADER_DEVOPS_BK_TICKET: String = "X-DEVOPS-BK-TICKET"
const val AUTH_HEADER_DEVOPS_TASK_ID: String = "X-DEVOPS-TASK-ID"
const val AUTH_HEADER_DEVOPS_APP_CODE: String = "X-DEVOPS-APP-CODE"
const val AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE: String = "admin"
const val AUTH_HEADER_DEVOPS_BUILD_ID: String = "x-devops-build-id"
const val AUTH_HEADER_DEVOPS_BK_TOKEN: String = "X-DEVOPS-BK-TOKEN"
const val AUTH_HEADER_DEVOPS_TOKEN: String = "X-DEVOPS-TOKEN"
const val AUTH_HEADER_DEVOPS_REPO_USER_ID = "X-BKREPO-UID"
const val AUTH_HEADER_DEVOPS_WEBHOOK_TOKEN = "X-DEVOPS-WEBHOOK-TOKEN"
const val AUTH_HEADER_DEVOPS_BK_GATEWAY_TAG = "X-GATEWAY-TAG"
const val AUTH_HEADER_DEVOPS_TOOL_IMAGE_TAG = "X-DEVOPS-TOOL-IMAGE-TAG"

/**
 * Build接口在内部调用中流转的Header
 * 入口是 BuildIdHeaderCacheEnterFilter 将 缓存线程的BuildId
 * Client 请求时判断是否存在BuildId缓存 决定是否调用Report服务
 *
 */
const val TRACE_HEADER_BUILD_ID = "TRACE-BUILD-ID"

/**
 * 链路ID
 */
const val TRACE_ID = "TRACE-ID"