package com.tencent.bk.codecc.openapi.resources

import com.tencent.bk.codecc.openapi.v2.OpenApiCallbackResource
import com.tencent.bk.codecc.openapi.v2.constant.ProjectEventType
import com.tencent.bk.codecc.openapi.v2.vo.ProjectCallbackEvent
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.devops.common.api.pojo.codecc.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.security.AuthCodeCCToken
import org.apache.commons.lang3.BooleanUtils
import org.slf4j.LoggerFactory
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

    override fun projectEvent(callBackInfo: ProjectCallbackEvent): Result<String> {
        // 仅处理项目禁用的情况，禁用后，项目下的扫描任务停用
        if (callBackInfo.event == ProjectEventType.PROJECT_DISABLE) {
            logger.info("projectId: ${callBackInfo.data.projectId} disable.")
            client.getWithoutRetry(ServiceTaskRestResource::class.java)
                    .stopDisableProjectTask(callBackInfo.data.projectId)
        }
        // 处理项目启用的情况，启用后，项目下的扫描任务重新启动，注意仅启用因为项目停用导致停用的扫描任务
        if (callBackInfo.event == ProjectEventType.PROJECT_ENABLE) {
            logger.info("projectId: ${callBackInfo.data.projectId} enable.")
            client.getWithoutRetry(ServiceTaskRestResource::class.java)
                .startEnableProjectTask(callBackInfo.data.projectId)
        }
        return Result(BooleanUtils.TRUE)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpenApiCallbackResourceImpl::class.java)
    }
}
