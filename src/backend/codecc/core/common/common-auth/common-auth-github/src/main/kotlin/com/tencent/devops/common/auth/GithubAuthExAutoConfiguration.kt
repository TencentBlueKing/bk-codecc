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

package com.tencent.devops.common.auth

import com.tencent.devops.common.auth.api.GithubAuthExPermissionApi
import com.tencent.devops.common.auth.api.GithubAuthExRegisterApi
import com.tencent.devops.common.auth.api.service.AuthTaskService
import com.tencent.devops.common.auth.pojo.GithubAuthProperties
import com.tencent.devops.common.client.Client
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.data.redis.core.RedisTemplate

@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "github")
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class GithubAuthExAutoConfiguration() {

    private val logger = LoggerFactory.getLogger(GithubAuthExAutoConfiguration::class.java)

    init {
        logger.info("use github auth config")
    }

    @Bean
    fun authExPermissionApi(
        redisTemplate: RedisTemplate<String, String>, client: Client, authTaskService: AuthTaskService,
        properties: GithubAuthProperties
    ) = GithubAuthExPermissionApi(client, redisTemplate, authTaskService, properties)

    @Bean
    @Primary
    fun authExRegisterApi(redisTemplate: RedisTemplate<String, String>, authTaskService: AuthTaskService,
                          client: Client, properties: GithubAuthProperties) =
        GithubAuthExRegisterApi(client, authTaskService, properties)

    @Bean
    @ConfigurationProperties(prefix = "auth.github")
    fun githubAuthProperties() = GithubAuthProperties()

}