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

package com.tencent.devops.common.web.security.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_TASK_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.exception.UnauthorizedException
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi
import com.tencent.devops.common.auth.api.external.CodeCCExtAuthProcessor
import com.tencent.devops.common.auth.api.external.GitPipelineTaskAuthProcessor
import com.tencent.devops.common.auth.api.external.GongfengScanTaskAuthProcessor
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import com.tencent.devops.common.auth.api.pojo.external.ResourceType
import com.tencent.devops.common.auth.api.pojo.external.UserGroupRole
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel
import com.tencent.devops.common.auth.api.service.AuthTaskService
import com.tencent.devops.common.auth.api.util.PermissionUtil
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import java.util.regex.Pattern
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter

class PermissionAuthFilter(
    private val resourceType: ResourceType,
    private val actions: List<CodeCCAuthAction>,
    private val roles: List<UserGroupRole>,
    private val extPassBeanName: String
) : ContainerRequestFilter {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PermissionAuthFilter::class.java)
    }

    override fun filter(requestContext: ContainerRequestContext) {
        val authExPermissionApi = SpringContextUtil.getBean(AuthExPermissionApi::class.java)
        val authTaskService = SpringContextUtil.getBean(AuthTaskService::class.java)
        val user = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_USER_ID)

        // 如果是管理员就直接校验通过
        if (authExPermissionApi.isAdminMember(user)) {
            return
        }

        /**
         * 判断是否符合 补充验证逻辑
         */
        if (extPassBeanName.isNotBlank() && !CodeCCExtAuthProcessor::class.java.simpleName.equals(extPassBeanName)) {
            val processor = try {
                SpringContextUtil.getBean(CodeCCExtAuthProcessor::class.java, lowerFirstChar(extPassBeanName))
            } catch (e: BeansException) {
                null
            } ?: throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(extPassBeanName))

            if (processor.isPassAuth(requestContext)) {
                return
            }
            logger.warn("extPassClassName[$extPassBeanName] is not pass auth! user: $user")
        }

        val projectId = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_PROJECT_ID)

        if (user.isNullOrBlank() || projectId.isNullOrBlank()) {
            logger.error("insufficient param info! user: $user, projectId: $projectId")
            throw UnauthorizedException("insufficient param info!")
        }

        // 项目级别用户组成员校验
        if (roles.isNotEmpty()) {
            val booleanList = roles.map { authExPermissionApi.authProjectRole(projectId, user, it.name) }
            if (checkBooleanListPass(booleanList)) {
                return
            }
            logger.warn(
                "project roles member auth fail: $user, $projectId, ${
                    roles.map { it.name }.joinToString {
                        ComConstants.SEMICOLON
                    }
                }"
            )
        }

        val result = when (resourceType) {
            ResourceType.PROJECT -> {
                val booleanList = actions.map {
                    authExPermissionApi.validateUserProjectPermission(projectId, user, it.actionName)
                }
                booleanList.map { BkAuthExResourceActionModel(isPass = it) }
            }

            ResourceType.TASK -> {
                // 当没获取到任务id时，只校验项目维度的权限
                val taskId = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_TASK_ID)
                if (taskId.isNullOrBlank()) {
                    logger.warn("permission auth task id isNullOrBlank")
                    val isProjectRole = authExPermissionApi.authProjectRole(projectId, user, role = null)
                    if (!isProjectRole) {
                        throw UnauthorizedException("unauthorized user permission!")
                    }
                    return
                }

                val taskCreateFrom = authTaskService.getTaskCreateFrom(taskId.toLong())
                logger.info("task create from: $taskCreateFrom, user: $user， projectId: $projectId")

                // 若是BG管理员则校验通过
                if (authExPermissionApi.isBgAdminMember(user, taskId, taskCreateFrom)) {
                    return
                }
                when (taskCreateFrom) {
                    ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() -> {
                        val pipelineAuthResults: MutableList<BkAuthExResourceActionModel> = mutableListOf()
                        // 工蜂CI项目没有在蓝鲸权限中心注册过，需要走Oauth鉴权与工蜂权限对齐
                        if (Pattern.compile("^git_[0-9]+$").matcher(projectId).find()) {
                            val authProcessor =
                                SpringContextUtil.getBean(GitPipelineTaskAuthProcessor::class.java) ?: return
                            pipelineAuthResults.add(
                                BkAuthExResourceActionModel(
                                    "pipeline_auth",
                                    null, null, authProcessor.isPassAuth(requestContext, actions)
                                )
                            )
                        } else {
                            // 普通流水线在蓝鲸权限中心鉴权
                            val pipelieActions = PermissionUtil.getPipelinePermissionsFromActions(actions)
                            val pipelinePermissionAuthResult = authExPermissionApi.validatePipelineBatchPermission(
                                user,
                                taskId,
                                projectId,
                                pipelieActions
                            )
                            var pipelineAuthPass = true
                            pipelinePermissionAuthResult.forEach {
                                if (it.isPass == false) {
                                    pipelineAuthPass = false
                                }
                            }

                            if (pipelineAuthPass) {
                                pipelineAuthResults.add(
                                    BkAuthExResourceActionModel(
                                        "pipeline_auth",
                                        null, null, true
                                    )
                                )
                            } else {
                                pipelineAuthResults.add(
                                    BkAuthExResourceActionModel(
                                        "pipeline_auth",
                                        null, null, false
                                    )
                                )
                            }
                        }
                        pipelineAuthResults
                    }

                    ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() -> {
                        val authProcessor =
                            SpringContextUtil.getBean(GongfengScanTaskAuthProcessor::class.java) ?: return
                        if (authProcessor.isPassAuth(requestContext, actions)) {
                            logger.info("gongfeng authorization pass, task id: $taskId")
                            return
                        } else {
                            logger.error("empty validate result: $user")
                            throw CodeCCException(CommonMessageCode.PERMISSION_DENIED, arrayOf(user))
                        }
                    }

                    else -> {
                        val codeccActions = PermissionUtil.getCodeCCPermissionsFromActions(actions)
                        authExPermissionApi.validateTaskBatchPermission(
                            user,
                            taskId,
                            projectId,
                            codeccActions
                        )
                    }
                }
            }

            else -> {
                logger.error("Unrecognized resource type: ${resourceType.id}")
                throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(resourceType.id))
            }
        }

        if (result.isNullOrEmpty()) {
            logger.error("empty validate result: $user")
            throw UnauthorizedException("unauthorized user permission!")
        }
        result.forEach {
            if (it.isPass == true) {
                return
            }
        }
        logger.error("validate permission fail! user: $user")
        throw UnauthorizedException("unauthorized user permission!")
    }

    private fun checkBooleanListPass(booleanList: List<Boolean>): Boolean {
        booleanList.forEach {
            if (it) {
                return true
            }
        }
        return false
    }

    /**
     * 类名首字母转小写
     */
    private fun lowerFirstChar(className: String): String {
        return className.replaceFirst(className[0], className[0].toLowerCase())
    }
}