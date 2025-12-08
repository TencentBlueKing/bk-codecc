package com.tencent.devops.common.auth.api.external

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter

/**
 * 用于补充业务鉴权的接口
 */
interface CodeCCExtAuthProcessor {

    fun isPassAuth(requestContext: ContainerRequestContext, filter: ContainerRequestFilter): Boolean
}
