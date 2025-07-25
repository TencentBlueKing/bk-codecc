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

package com.tencent.devops.common.web.handler

import com.tencent.devops.common.api.pojo.codecc.Result
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.utils.I18NUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.jmx.exception.JmxExceptions
import org.slf4j.LoggerFactory
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

/**
 * Created by liangyuzhou on 2017/9/2.
 * Powered By Tencent
 */
@Provider
class RuntimeExceptionMapper : ExceptionMapper<RuntimeException> {
    companion object {
        val logger = LoggerFactory.getLogger(RuntimeExceptionMapper::class.java)!!
    }

    override fun toResponse(exception: RuntimeException): Response {
        logger.error("Failed with runtime exception", exception)
        val status = Response.Status.INTERNAL_SERVER_ERROR
        val message = if (SpringContextUtil.getBean(Profile::class.java).isDev()) {
            exception.message ?: exception.toString()
        } else {
            I18NUtils.getMessage(CommonMessageCode.UNKNOWN_ERROR)
        }

        JmxExceptions.encounter(exception)

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(
                    Result<Void>(
                        status = status.statusCode,
                        errCode = CommonMessageCode.UNKNOWN_ERROR,
                        message = message
                    )
                )
                .build()
    }
}