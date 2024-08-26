package com.tencent.bk.codecc.openapi.resources

import com.tencent.bk.codecc.openapi.v2.OpenApiCallbackResource
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.security.AuthCodeCCToken
import org.springframework.beans.factory.annotation.Autowired

@AuthCodeCCToken
@RestResource
class OpenApiCallbackResourceImpl @Autowired constructor(
    private val client: Client
) : OpenApiCallbackResource {

    /**
     * 蓝盾迁移权限系统需用到的回调接口
     */
    override fun resourceList(callBackInfo: CallbackRequestDTO): String {
        return client.getWithoutRetry(ServiceTaskRestResource::class.java).resourceList(callBackInfo).data!!
    }
}
