package com.tencent.devops.common.auth.api.external

import com.tencent.devops.common.auth.api.pojo.external.AuthRoleType
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import com.tencent.devops.common.auth.api.pojo.external.TaskAuthInfo
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface AuthExPermissionApi {

    /**
     * 查询指定用户特定权限下的流水线清单
     */
    fun queryPipelineListForUser(
        user: String,
        projectId: String,
        actions: Set<String>
    ): Set<String>

    /**
     * 查询指定用户特定权限下的代码检查任务清单
     */
    fun queryTaskListForUser(
        user: String,
        projectId: String,
        actions: Set<String>
    ): Set<String>

    /**
     * 查询指定流水线下特定权限的用户清单
     */
    fun queryPipelineUserListForAction(
        taskId: String,
        projectId: String,
        actions: Set<String>
    ): List<String>

    /**
     * 查询指定代码检查任务下特定权限的用户清单
     */
    fun queryTaskUserListForAction(
        taskId: String,
        projectId: String,
        actions: Set<String>
    ): List<String>

    /**
     * 批量校验权限
     */
    fun validatePipelineBatchPermission(
        user: String,
        taskId: String,
        projectId: String,
        actions: Set<String>
    ): List<BkAuthExResourceActionModel>

    /**
     * 批量校验权限
     */
    fun validatePipelinesBatchPermission(
        user: String,
        pipelineIds: List<String>,
        projectId: String,
        actions: Set<String>
    ): List<BkAuthExResourceActionModel>

    /**
     * 批量校验权限
     */
    fun validateTaskBatchPermission(
        user: String,
        taskId: String,
        projectId: String,
        actions: Set<String>
    ): List<BkAuthExResourceActionModel>

    /**
     * 批量校验权限
     */
    fun validateTasksBatchPermission(
        user: String,
        taskIds: List<String>,
        projectId: String,
        actions: Set<String>
    ): List<BkAuthExResourceActionModel>

    /**
     * 校验用户是否是管理员
     */
    fun isAdminMember(
        user: String
    ): Boolean

    /**
     * 校验用户是否是管理员
     */
    fun getAdminMembers(): List<String>

    /**
     * 校验用户是否是工具开发者
     */
    fun isToolDeveloper(
        user: String,
        taskId: String,
    ): Boolean

    /**
     * 校验用户是否是BG管理员
     */
    fun isBgAdminMember(
        user: String,
        taskId: String,
        createFrom: String?
    ): Boolean

    /**
     * 校验是否项目管理员
     *
     * @param projectId
     * @param user
     * @return
     */
    fun authProjectManager(projectId: String, user: String): Boolean


    /**
     * 校验是否项目管理员(不抛出鉴权异常)，如果失败就是false
     *
     * @param projectId
     * @param user
     * @return
     */
    fun authProjectManagerNotThrow(projectId: String, user: String): Boolean {
        return try {
            authProjectManager(projectId, user)
        } catch (e: Exception) {
            logger.error("auth project manager cause error. ${e.message}")
            false
        }
    }

    /**
     * 获取项目管理员列表
     *
     * @param projectId
     */
    fun getProjectManager(projectId: String): List<String>

    /**
     * 校验是否项目角色
     */
    fun authProjectMultiManager(projectId: String, user: String): Boolean

    /**
     * 校验是否项目角色
     */
    fun authProjectRole(projectId: String, user: String, role: String?): Boolean

    /**
     * 校验代码问题操作权限
     */
    fun authDefectOpsPermissions(
        taskId: Long,
        projectId: String,
        username: String,
        createFrom: String,
        actions: List<CodeCCAuthAction>
    ): Boolean

    /**
     * 校验代码问题操作权限
     */
    fun authDefectOpsPermissions(
        taskAuthInfos: List<TaskAuthInfo>,
        projectId: String,
        username: String,
        actions: List<CodeCCAuthAction>
    ): List<BkAuthExResourceActionModel>

    /**
     * 判断项目是否已迁移到RBAC
     */
    fun checkProjectIsRbacPermissionByCache(projectId: String, needRefresh: Boolean? = false): Boolean

    /**
     * 查询扫描服务创建的任务角色成员清单
     */
    fun queryTaskUsersGroupByRole(projectId: String, taskId: Long, roleType: AuthRoleType?): Set<String>

    /**
     * 查询流水线创建的任务角色成员清单
     */
    fun queryPipelineUsersGroupByRole(projectId: String, taskId: Long, roleType: AuthRoleType?): Set<String>

    /**
     * 校验用户是否有项目维度的资源操作权限
     */
    fun validateUserProjectPermission(
        projectId: String,
        userId: String,
        action: String
    ): Boolean

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AuthExPermissionApi::class.java)
    }
}
