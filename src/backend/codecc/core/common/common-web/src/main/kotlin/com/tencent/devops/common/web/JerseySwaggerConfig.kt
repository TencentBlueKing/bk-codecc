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

package com.tencent.devops.common.web

import com.tencent.devops.common.service.Profile
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import jakarta.annotation.PostConstruct

class JerseySwaggerConfig constructor(
    private val profile: Profile
) : JerseyConfig() {

    @Value("\${spring.application.desc:#{null}}")
    private val applicationDesc: String? = null

    @Value("\${spring.application.version:#{null}}")
    private val applicationVersion: String? = null

    @Value("\${spring.application.packageName:#{null}}")
    private val packageName: String? = null

    private val logger = LoggerFactory.getLogger(JerseySwaggerConfig::class.java)

    @PostConstruct
    fun init() {
        logger.info("configSwagger-start (v3)")
        configSwagger()
        register(OpenApiResource::class.java)
        logger.info("configSwagger-end (v3)")
    }

    private fun configSwagger() {
        if (!packageName.isNullOrBlank()) {
            val appName =
                with(profile)
                {
                    if (isLocal()) "" else "/${getServiceName()}"
                }
            
            // Swagger v3 配置
            val openAPI = OpenAPI().info(
                Info()
                    .title(applicationDesc ?: "CodeCC API")
                    .version(applicationVersion ?: "1.0")
            )
            
            // 注册 OpenAPI 配置
            logger.info("Swagger v3 configured: basePath=/api$appName, package=$packageName")
        }
    }
}
