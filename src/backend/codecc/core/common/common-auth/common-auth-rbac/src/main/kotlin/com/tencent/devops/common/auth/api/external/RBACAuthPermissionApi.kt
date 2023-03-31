package com.tencent.devops.common.auth.api.external

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.exception.UnauthorizedException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.common.util.OkhttpUtils
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate

class RBACAuthPermissionApi(
    client: Client,
    redisTemplate: RedisTemplate<String, String>,
    private val authPropertiesData: RBACAuthPropertiesData
) : AbstractAuthExPermissionApi(
    client,
    redisTemplate
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RBACAuthPermissionApi::class.java)
        private const val baseUrl = "/ms/auth/api"
    }

    /**
     * http方式的公共请求头
     */
    private fun getCommonHeaders(projectId: String?): Map<String, String> {
        return mapOf(
            AUTH_HEADER_DEVOPS_BK_TOKEN to getBackendAccessToken(),
            AUTH_HEADER_DEVOPS_PROJECT_ID to (projectId ?: ""),
            "accept" to "application/json",
            "contentType" to "application/json"
        )
    }

    /**
     * 查询非用户态Access Token
     */
    private fun getBackendAccessToken(): String {
        return authPropertiesData.token ?: ""
    }

    /**
     * 查询指定用户特定权限下的流水线清单
     */
    override fun queryPipelineListForUser(user: String, projectId: String, actions: Set<String>): Set<String> {
        // TODO
        val newAction = "${authPropertiesData.pipelineResourceType!!}_${actions.first()}"
        return queryUserResourceByPermission(
            projectCode = projectId,
            action = newAction,
            resourceType = authPropertiesData.pipelineResourceType!!,
            userId = user
        ).toSet()
    }

    /**
     * 查询指定用户特定权限下的代码检查任务清单
     */
    override fun queryTaskListForUser(
        user: String,
        projectId: String,
        actions: Set<String>
    ): Set<String> {
        return queryUserResourceByPermission(
            projectCode = projectId,
            action = actions.first(),
            resourceType = authPropertiesData.rbacResourceType!!,
            userId = user
        ).toSet()
    }

    /**
     * 查询指定代码检查任务下特定权限的用户清单 TODO 会议讨论结果：V3不支持，待定
     */
    override fun queryTaskUserListForAction(taskId: String, projectId: String, actions: Set<String>): List<String> {
        TODO("Not yet implemented")
    }

    /**
     * 批量校验权限
     */
    override fun validatePipelineBatchPermission(
        user: String,
        taskId: String,
        projectId: String,
        actions: Set<String>
    ): List<BkAuthExResourceActionModel> {
        val authTaskService = SpringContextUtil.getBean(AuthTaskService::class.java)
        val pipelineId = authTaskService.getTaskPipelineId(taskId.toLong())

        val result = validateBatch(
            projectCode = projectId,
            resourceCode = pipelineId,
            resourceType = authPropertiesData.pipelineResourceType!!,
            actions = actions.toList(),
            userId = user
        )

        if (result.isNotOk()) {
            throw UnauthorizedException("getDevopsService failed!")
        }
        return listOf(BkAuthExResourceActionModel(isPass = (result.data ?: false)))
    }

    /**
     * 批量校验权限
     */
    override fun validateTaskBatchPermission(
        user: String,
        taskId: String,
        projectId: String,
        actions: Set<String>
    ): List<BkAuthExResourceActionModel> {
        val result = validateBatch(
            projectCode = projectId,
            resourceCode = taskId,
            resourceType = authPropertiesData.rbacResourceType!!,
            actions = actions.toList(),
            userId = user
        )

        if (result.isNotOk()) {
            throw UnauthorizedException("getDevopsService failed!")
        }
        return listOf(BkAuthExResourceActionModel(isPass = (result.data ?: false)))
    }

    /**
     * 校验工蜂权限
     */
    override fun validateGongfengPermission(
        user: String,
        taskId: String,
        projectId: String,
        actions: List<CodeCCAuthAction>
    ): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * 校验是否项目管理员
     *
     * @param projectId
     * @param user
     * @return
     */
    override fun authProjectManager(projectId: String, user: String): Boolean {
        return checkProjectManager(projectId, user)
    }

    /**
     * 校验用户是否项目中的角色成员
     * Available values : CIADMIN, MANAGER, DEVELOPER, MAINTAINER, TESTER, PM, QC, CI_MANAGER
     */
    override fun authProjectRole(projectId: String, user: String, role: String?): Boolean {
        val url = "https://${authPropertiesData
                .url}$baseUrl/open/service/auth/projects/${projectId}/users/${user}/isProjectUsers"
        val headers = getCommonHeaders(projectId)
        val params = mutableMapOf<String, String>()
        // 选填，判断是否有访问项目的权限，只要加入项目下 任意一个组，都会是项目的成员
        if (!role.isNullOrBlank()) {
            params["group"] = role
        }
        val resultStr = OkhttpUtils.doGet(url, params, headers)
        val result = JsonUtil.to(resultStr, object : TypeReference<Result<Boolean>>() {})
        if (result.isNotOk()) {
            throw UnauthorizedException("getDevopsService failed!")
        }
        return result.data ?: false
    }


    /**
     * 获取用户某项目下指定资源action的实例列表
     */
    private fun queryUserResourceByPermission(
        projectCode: String,
        action: String,
        resourceType: String,
        userId: String
    ): List<String> {
        val response = client.getDevopsService(ServicePermissionAuthResource::class.java, projectCode)
            .getUserResourceByPermission(
                userId = userId,
                token = getBackendAccessToken(),
                action = action,
                projectCode = projectCode,
                resourceType = resourceType
            )
        if (response.isNotOk()) {
            throw UnauthorizedException("getDevopsService failed!")
        }

        return response.data ?: listOf()
    }

    /**
     * 校验用户是否是项目管理员
     */
    private fun checkProjectManager(
        projectCode: String,
        userId: String
    ): Boolean {
        val url = "https://${authPropertiesData.url}$baseUrl/open/service/auth/projects/${projectCode}" +
                "/users/${userId}/checkProjectManager"
        val headers = getCommonHeaders(projectCode).toMutableMap()
        headers[AUTH_HEADER_DEVOPS_USER_ID] = userId
        val result = OkhttpUtils.doGet(url, headers = headers)
        logger.info("checkProjectManager result: $result")
        val response = JsonUtil.to(result, object : TypeReference<Result<Boolean>>() {})
        if (response.isNotOk()) {
            throw UnauthorizedException("getDevopsService failed!")
        }
        return response.data ?: false
    }


    /**
     * 批量校验用户是否有具体资源实例的操作权限
     */
    private fun validateBatch(
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        actions: List<String>,
        userId: String
    ): Result<Boolean> {
        val url = "https://${authPropertiesData.url}$baseUrl/open/service/auth/permission/projects/${projectCode}" +
            "/relation/validate/batch?resourceCode=${resourceCode}&resourceType=${resourceType}"
        val headers = getCommonHeaders(projectCode).toMutableMap()
        headers[AUTH_HEADER_DEVOPS_USER_ID] = userId
        val bodyStr = JsonUtil.getObjectMapper().writeValueAsString(actions)
        val result = OkhttpUtils.doHttpPost(url, body = bodyStr, headers = headers)
        logger.info("validateBatch result: $result")
        return JsonUtil.to(result, object : TypeReference<Result<Boolean>>() {})
    }
}