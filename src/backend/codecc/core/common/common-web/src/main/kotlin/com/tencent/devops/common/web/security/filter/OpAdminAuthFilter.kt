/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 */

package com.tencent.devops.common.web.security.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.exception.UnauthorizedException
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter

/**
 * 兜底过滤器：用于框架自动识别出的 OP 路径（`/op/...`）但接口或实现未声明 `@AuthMethod` 的资源。
 *
 * 校验请求头 `X-DEVOPS-UID`：
 *   1. 若运行时存在 OpAuthApi（独立 OP 管理员名单）的 bean，则要求是 OP 管理员；
 *   2. 否则回退到业务管理员名单 `AuthExPermissionApi.isAdminMember`；
 *   3. 都不通过则拒绝。
 *
 * 具体接口若有更严格的限制（个人/项目维度），仍由实现层处理；OpAuthApi 通过反射访问，
 * 避免在 common-web 中硬依赖 common-auth-op。
 */
class OpAdminAuthFilter : ContainerRequestFilter {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(OpAdminAuthFilter::class.java)
        private const val OP_AUTH_API_CLASS = "com.tencent.devops.common.auth.api.OpAuthApi"
    }

    override fun filter(requestContext: ContainerRequestContext) {
        val user = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_USER_ID)
        if (user.isNullOrBlank()) {
            logger.error("op route rejected: missing $AUTH_HEADER_DEVOPS_USER_ID, path: ${requestContext.uriInfo.path}")
            throw UnauthorizedException("missing user header")
        }

        if (resolveOpAdmin(user)) {
            return
        }

        val authExPermissionApi = try {
            SpringContextUtil.getBean(AuthExPermissionApi::class.java)
        } catch (e: Exception) {
            null
        }
        if (authExPermissionApi != null && authExPermissionApi.isAdminMember(user)) {
            return
        }

        logger.error("op route rejected: not an admin, user: $user, path: ${requestContext.uriInfo.path}")
        throw UnauthorizedException("admin required")
    }

    /**
     * 反射调用 OpAuthApi.isOpAdminMember，避免 common-web 引入 common-auth-op 依赖。
     * 当宿主服务未引入 common-auth-op 或未注册 bean 时，返回 false 让调用方走 fallback。
     */
    @Suppress("UNCHECKED_CAST")
    private fun resolveOpAdmin(user: String): Boolean {
        return try {
            val clazz = Class.forName(OP_AUTH_API_CLASS) as Class<Any>
            val bean = SpringContextUtil.getBean(clazz)
            val method = clazz.getMethod("isOpAdminMember", String::class.java)
            (method.invoke(bean, user) as? Boolean) ?: false
        } catch (e: ClassNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
