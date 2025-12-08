package com.tencent.devops.common.auth.api

import com.tencent.devops.common.auth.api.external.AbstractAuthExPermissionApi
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.util.List2StrUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate

class OpAuthApi @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, String>
){
    companion object{
        /**
         * OP管理员名单
         */
        val KEY_OP_ADMIN_MEMBER = "OP_ADMIN_MEMBER"

        private val logger = LoggerFactory.getLogger(AbstractAuthExPermissionApi::class.java)
    }


    /**
     * 校验是否是op管理员
     */
    fun isOpAdminMember(
        user: String
    ): Boolean {
        logger.debug("judge user is op admin member: {}", user)
        val adminMemberStr = redisTemplate.opsForValue().get(KEY_OP_ADMIN_MEMBER)
        val adminMembers = List2StrUtil.fromString(adminMemberStr, ComConstants.SEMICOLON)
        return if (adminMembers.contains(user)) {
            logger.debug("Is Op admin member: {}", user)
            true
        } else {
            logger.debug("Not Op admin member: {}", user)
            false
        }
    }
}
