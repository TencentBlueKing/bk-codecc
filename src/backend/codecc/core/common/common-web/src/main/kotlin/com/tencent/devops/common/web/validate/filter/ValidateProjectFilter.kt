package com.tencent.devops.common.web.validate.filter

import com.alibaba.fastjson.JSONObject
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter

class ValidateProjectFilter : ContainerRequestFilter {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ValidateProjectFilter::class.java)
    }

    override fun filter(requestContext: ContainerRequestContext) {
        val projectHeader = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_PROJECT_ID)
        if (projectHeader.isNullOrEmpty()) {
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, arrayOf("projectId"))
        }
        val client = SpringContextUtil.getBean(Client::class.java)
        val result = client.getDevopsService(ServiceProjectResource::class.java, projectHeader).get(projectHeader)
        if (result.isNotOk()) {
            logger.error(
                "ValidateProjectFilter projectId : $projectHeader get info error. " +
                        "result: ${JSONObject.toJSONString(result)}"
            )
            throw CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL)
        }
        if (result.data == null) {
            logger.error("ValidateProjectFilter projectId : $projectHeader get data null. ")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("projectId"))
        }
    }
}