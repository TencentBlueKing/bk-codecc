/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.common.web.handler

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.constant.CommonMessageCode
import org.slf4j.LoggerFactory
import javax.validation.ConstraintViolationException
import javax.validation.ValidationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

@Provider
class ValidationExceptionMapper : ExceptionMapper<ValidationException> {

    companion object {
        val logger = LoggerFactory.getLogger(ValidationExceptionMapper::class.java)!!
    }

    override fun toResponse(exception: ValidationException): Response {
        val status = Response.Status.OK
        val response = Response.status(status).type(MediaType.APPLICATION_JSON_TYPE)
        if (exception is ConstraintViolationException) {

            val message = exception.constraintViolations.map { constraintViolation ->
                constraintViolation.message
            }.reduce { acc, s -> "$acc|$s" }
            response.entity(Result<Void>(
                    status = status.statusCode,
                    errCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    message = message))
        }
        return response.build()
    }
}