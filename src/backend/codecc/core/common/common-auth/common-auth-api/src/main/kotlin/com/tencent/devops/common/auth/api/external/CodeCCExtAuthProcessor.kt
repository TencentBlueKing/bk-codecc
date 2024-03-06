package com.tencent.devops.common.auth.api.external

import javax.ws.rs.container.ContainerRequestContext

/**
 * 用于补充业务鉴权的接口
 */
interface CodeCCExtAuthProcessor {

    fun isPassAuth(requestContext: ContainerRequestContext): Boolean
}
