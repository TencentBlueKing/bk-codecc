/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.common.auth.api.external

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.exception.UnauthorizedException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.request.RBACAuthResourceDeleteRequest
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.pojo.AllProperties
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.common.util.OkhttpUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class RBACAuthRegisterApi @Autowired constructor(
    private val client: Client,
    private val rbacAuthProperties: RBACAuthProperties,
    private val allProperties: AllProperties
) : AuthExRegisterApi {

    companion object {
        private val logger = LoggerFactory.getLogger(RBACAuthRegisterApi::class.java)
        private const val baseUrl = "/ms/auth/api"
    }

//    @Value("\${auth.v3.token:#{null}}")
//    private val authTokenV3: String? = null

    /**
     * 查询非用户态Access Token
     */
    private fun getBackendAccessToken(): String {
        return rbacAuthProperties.token ?: ""
    }

    /**
     * http方式的公共请求头
     */
    private fun getCommonHeaders(projectId: String?): Map<String, String> {
        return mapOf(
            AUTH_HEADER_DEVOPS_BK_TOKEN to getBackendAccessToken(),
            AUTH_HEADER_DEVOPS_TOKEN to (allProperties.devopsToken ?: ""),
            AUTH_HEADER_DEVOPS_PROJECT_ID to (projectId ?: ""),
            "accept" to "application/json",
            "contentType" to "application/json"
        )
    }

    /**
     * 注册代码检查任务
     */
    override fun registerCodeCCTask(
        user: String,
        taskId: String,
        taskName: String,
        projectId: String
    ): Boolean {
        logger.info("register RBAC resource: $user, $taskId, $projectId")
        val result = registerResource(
                projectId = projectId,
                resourceCode = taskId,
                resourceName = taskName,
                resourceType = rbacAuthProperties.rbacResourceType!!,
                creator = user
        )
        if (result.isNotOk() || result.data == null || result.data == false) {
            logger.error("register resource failed! taskId: $taskId, return code:${result.code}," +
                " err message: ${result.message}")
            throw CodeCCException(CommonMessageCode.PERMISSION_DENIED, arrayOf(user))
        }
        return true
    }

    /**
     * 删除代码检查任务
     */
    override fun deleteCodeCCTask(
        taskId: String,
        projectId: String
    ): Boolean {
        logger.info("deleteCodeCCTask >>>>>>>>>>>>> $taskId, $projectId")
        val result = deleteResource(
                projectCode = projectId,
                resourceCode = taskId,
                resourceType = rbacAuthProperties.rbacResourceType!!
        )
        if (result.isNotOk() || result.data == null || result.data == false) {
            logger.error("delete resource failed! taskId: $taskId, return code:${result.code}," +
                " err message: ${result.message}")
            throw UnauthorizedException("delete resource failed!")
        }
        return true
    }

    /**
     * 调用api注册资源
     */
    private fun registerResource(
        projectId: String,
        resourceCode: String,
        resourceName: String,
        resourceType: String,
        creator: String
    ): Result<Boolean> {
        logger.info("resourceType >>>>>>>>>>>>>>>>> $resourceType")
        return client.getDevopsService(ServicePermissionAuthResource::class.java, projectId).resourceCreateRelation(
            userId = creator,
            token = getBackendAccessToken(),
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName
        )
    }


    /**
     * 调用api删除资源
     */
    private fun deleteResource(
            projectCode: String,
            resourceCode: String,
            resourceType: String
    ): Result<Boolean> {
        val url = "https://${rbacAuthProperties
            .url}$baseUrl/open/service/auth/permission/projects/$projectCode/delete/relation"
        val deleteRequest = RBACAuthResourceDeleteRequest(
            resourceCode = resourceCode,
            resourceType = resourceType
        )
        val content = JsonUtil.getObjectMapper().writeValueAsString(deleteRequest)
        val result = OkhttpUtils.doHttpDelete(url, content, getCommonHeaders(projectCode))
        return JsonUtil.to(result, object : TypeReference<Result<Boolean>>() {})
    }
}
