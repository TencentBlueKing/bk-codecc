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

package com.tencent.devops.common.redis.lock

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class RedisLock(
    private val redisTemplate: RedisTemplate<String, String>,
    private val lockKey: String,
    private val expiredTimeInSeconds: Long
) : AutoCloseable {
    companion object {
        /**
         * 调用set后的返回值
         */
        private const val OK = "OK"

        private val logger = LoggerFactory.getLogger(RedisLock::class.java)

        private val UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"
    }

    private val lockValue = UUID.randomUUID().toString()

    private var locked = false

    /**
     * 尝试获取锁 立即返回
     *
     * @return 是否成功获得锁
     */
    fun lock() {
        lock(null)
    }

    @Throws(TimeoutException::class)
    fun lock(timeoutMillis: Long?) {
        val start = System.currentTimeMillis()
        while (true) {
            logger.info("Start to lock($lockKey) of value($lockValue) for $expiredTimeInSeconds sec")
            val result = set(lockKey, lockValue, expiredTimeInSeconds)
            logger.info("Get the lock result($result)")
            val l = OK.equals(result, true)
            if (l) {
                locked = true
                return
            }
            val curTime = System.currentTimeMillis()
            if (timeoutMillis != null && (curTime - start > timeoutMillis)) {
                throw TimeoutException("get lock wait timeout")
            }
            Thread.sleep(100)
        }
    }

    fun tryLock(): Boolean {
        // 不存在则添加 且设置过期时间（单位ms）
        logger.info("Start to lock($lockKey) of value($lockValue) for $expiredTimeInSeconds sec")
        val result = set(lockKey, lockValue, expiredTimeInSeconds)
        logger.info("Get the lock result($result)")
        locked = OK.equals(result, true)
        return locked
    }

    /**
     * 重写 redisTemplate 的 set 方法，兼容 Lettuce
     * <p>
     * 命令 SET resource-name anystring NX EX max-lock-time 是一种在 Redis 中实现锁的简单方法。
     * <p>
     * 客户端执行以上的命令：
     * <p>
     * 如果服务器返回 OK ，那么这个客户端获得锁。
     * 如果服务器返回 NIL ，那么客户端获取锁失败，可以在稍后再重试。
     *
     * @param key 锁的Key
     * @param value 锁里面的值
     * @param seconds 过期时间（秒）
     * @return OK 或 null
     */
    private fun set(key: String, value: String, seconds: Long): String? {
        val ok = redisTemplate.opsForValue().setIfAbsent(key, value,seconds, TimeUnit.SECONDS) ?: false
        return if (ok) OK else null
    }

    /**
     * 解锁 (兼容 Lettuce)
     * <p>
     * 可以通过以下修改，让这个锁实现更健壮：
     * <p>
     * 不使用固定的字符串作为键的值，而是设置一个不可猜测（non-guessable）的长随机字符串，作为口令串（token）。
     * 不使用 DEL 命令来释放锁，而是发送一个 Lua 脚本，这个脚本只在客户端传入的值和键的口令串相匹配时，才对键进行删除。
     * 这两个改动可以防止持有过期锁的客户端误删现有锁的情况出现。
     */
    fun unlock(): Boolean {
        // 只有加锁成功并且锁还有效才去释放锁
        try {
            if (!unLockRemote()) {
                logger.warn("remote lock has changed , key: $lockKey , value: $lockValue")
                return false
            }
            return true
        } catch (e: Exception) {
            logger.error("unlock error", e)
            return unLockRemote() // try again
        }
    }

    private fun unLockRemote(): Boolean {
        return redisTemplate.execute(
            DefaultRedisScript(UNLOCK_LUA, Long::class.java),
            listOf(lockKey),
            lockValue
        ) > 0
    }

    /**
     * 获取当前上锁状态
     */
    fun isLocked(): Boolean {
        return locked
    }

    override fun close() {
        unlock()
    }
}