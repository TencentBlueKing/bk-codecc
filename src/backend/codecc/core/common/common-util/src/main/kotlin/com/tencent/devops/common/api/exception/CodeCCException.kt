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

package com.tencent.devops.common.api.exception

/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
open class CodeCCException(
        val errorCode: String,
        val params: Array<String>? = emptyArray(),
        val defaultMessage: String? = null,
        val errorCause: Throwable? = null
) : RuntimeException(defaultMessage, errorCause) {
    constructor(errCode: String, msgParam: Array<String>? = emptyArray(), errorCause: Throwable? = null) :
            this(errCode, msgParam, "访问接口失败，请重试或联系管理员", errorCause)

    constructor(errCode: String, msgParam: Array<String>? = emptyArray()) :
        this(errCode, msgParam, "访问接口失败，请重试或联系管理员", null)

    constructor(errCode: String) :
            this(errCode, emptyArray(), "访问接口失败，请重试或联系管理员", null)

    constructor(errCode: String, message: String) :
            this(errCode, emptyArray(), message, null)

    constructor(errCode: String, message: String, errorCause: Throwable? = null) :
            this(errCode, emptyArray(), message, errorCause)

    constructor(errCode: String, errorCause: Throwable) :
            this(errCode, emptyArray(), null, errorCause)
}