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

package com.tencent.devops.common.redis

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericToStringSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.net.UnknownHostException

/**
 * Redis 通用自动配置
 *
 * 说明：
 * 1. RedisConnectionFactory 使用 Spring Boot 3.3.2 自动配置（实际类型为 LettuceConnectionFactory）
 * 2. StringRedisTemplate 使用 Spring Boot 3.3.2 自动配置
 * 3. RedisTemplate 需要自定义序列化器，确保 Key/Value 可读性
 *
 * 注意：
 * - 使用 @AutoConfigureAfter 确保在 RedisAutoConfiguration 之后执行
 * - 这样可以依赖 Spring Boot 自动配置的 RedisConnectionFactory
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration::class)
class RedisCommonAutoConfiguration {

    /**
     * 自定义 RedisTemplate，配置字符串序列化器
     *
     * 为什么需要自定义：
     * - Spring Boot 默认的 RedisTemplate 使用 JdkSerializationRedisSerializer
     * - 会导致 Key 存储为二进制格式，不可读：\xAC\xED\x00\x05t\x00\x04myKey
     * - 使用 StringRedisSerializer 确保 Key/Value 可读性和与 Redis 命令行的兼容性
     *
     * 注意：
     * - RedisConnectionFactory 由 Spring Boot 自动配置，实际类型为 LettuceConnectionFactory
     * - StringRedisTemplate 由 Spring Boot 自动配置，无需手动创建
     */
    @Bean
    @ConditionalOnMissingBean(name = ["redisTemplate"])
    @Throws(UnknownHostException::class)
    fun redisTemplate(@Autowired redisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        val redisTemplate = RedisTemplate<String, String>()
        redisTemplate.setConnectionFactory(redisConnectionFactory)
        redisTemplate.setDefaultSerializer(StringRedisSerializer())
        redisTemplate.keySerializer = StringRedisSerializer()
        GenericToStringSerializer(String::class.java).also { redisTemplate.valueSerializer = it }
        return redisTemplate
    }

    // StringRedisTemplate 使用 Spring Boot 自动配置，无需定义
    // Spring Boot 会自动创建：StringRedisTemplate(RedisConnectionFactory)
}