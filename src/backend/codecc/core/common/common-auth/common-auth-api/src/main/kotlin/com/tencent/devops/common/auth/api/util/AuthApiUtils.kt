package com.tencent.devops.common.auth.api.util

import com.tencent.devops.common.auth.api.pojo.external.KEY_ADMIN_MEMBER
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.KEY_OP_ADMIN_MEMBER
import com.tencent.devops.common.util.List2StrUtil
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate

object AuthApiUtils {

    private val logger = LoggerFactory.getLogger(AuthApiUtils::class.java)

    fun isAdminMember(
        redisTemplate: RedisTemplate<String, String>,
        user: String
    ): Boolean {
        return isAdminMemberByType(user, KEY_ADMIN_MEMBER, redisTemplate)
    }

    /**
     * 校验是否是op管理员
     */
    fun isOpAdminMember(
        redisTemplate: RedisTemplate<String, String>,
        user: String
    ): Boolean {
        return isAdminMemberByType(user, KEY_OP_ADMIN_MEMBER, redisTemplate)
    }

    /**
     * 根据管理员类型来判断是否是管理员
     */
    private fun isAdminMemberByType(
        user: String,
        type: String,
        redisTemplate: RedisTemplate<String, String>
    ): Boolean {
        logger.debug("judge user is admin member: {}", user)
        val adminMemberStr = redisTemplate.opsForValue().get(type)
        val adminMembers = List2StrUtil.fromString(adminMemberStr, ComConstants.SEMICOLON)
        return if (adminMembers.contains(user)) {
            logger.debug("Is admin member: {}", user)
            true
        } else {
            logger.debug("Not admin member: {}", user)
            false
        }
    }

    fun getAdminMember(
        redisTemplate: RedisTemplate<String, String>
    ): List<String> {
        val adminMemberStr = redisTemplate.opsForValue().get(KEY_ADMIN_MEMBER)
        return List2StrUtil.fromString(adminMemberStr, ComConstants.SEMICOLON)
    }
}
