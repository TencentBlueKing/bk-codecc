package com.tencent.bk.codecc.codeccjob.component

import org.redisson.Redisson
import org.redisson.api.RRateLimiter
import org.redisson.api.RateIntervalUnit
import org.redisson.api.RateType
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class RedissionConfig {

    @Value("\${spring.redis.host}")
    private lateinit var redisHost: String

    @Value("\${spring.redis.port}")
    private lateinit var redisPort: String

    @Value("\${spring.redis.password}")
    private lateinit var redisPassword: String

    @Value("\${spring.redis.database:0}")
    private var redisDB: Int = 0

    @Value("\${spring.redis.ssl:#{null}}")
    private var redisSsl: String? = null

    @Value("\${issue.submit.limit.intervalLimit:25}")
    private var intervalLimit: Long = 25

    @Value("\${issue.submit.limit.intervalSeconds:1}")
    private var intervalSeconds: Long = 1

    @Bean
    fun issueSubmitRateLimiter(): RRateLimiter {
        val config = Config()
        val serviceConfig = if (!redisSsl.isNullOrBlank() && redisSsl.equals("true")) {
            config.useSingleServer().setAddress("rediss://$redisHost:$redisPort")
        } else {
            config.useSingleServer().setAddress("redis://$redisHost:$redisPort")
        }
        serviceConfig.setPassword(redisPassword).database = redisDB
        val client = Redisson.create(config)
        val rateLimiter = client.getRateLimiter("SUBMIT_ISSUE_RATE_LIMITER")
        rateLimiter.trySetRate(RateType.OVERALL, intervalLimit, intervalSeconds, RateIntervalUnit.SECONDS)
        return rateLimiter
    }

    @Bean
    fun redissionClient(): RedissonClient {
        val config = Config()
        val serviceConfig = if (!redisSsl.isNullOrBlank() && redisSsl.equals("true")) {
            config.useSingleServer().setAddress("rediss://$redisHost:$redisPort")
        } else {
            config.useSingleServer().setAddress("redis://$redisHost:$redisPort")
        }
        serviceConfig.setPassword(redisPassword).database = redisDB
        return Redisson.create(config)
    }
}