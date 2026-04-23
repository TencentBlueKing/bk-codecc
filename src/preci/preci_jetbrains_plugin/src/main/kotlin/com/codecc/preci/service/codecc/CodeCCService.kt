package com.codecc.preci.service.codecc

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * CodeCC 远程代码检查服务接口
 *
 * 负责与 CodeCC 平台交互，获取远程任务列表和缺陷列表。
 * 通过 PreCI Local Server 代理访问 CodeCC 后端，需要 OAuth 认证。
 *
 * **核心功能：**
 *
 * 1. **任务列表查询**
 *    - 获取当前用户在 CodeCC 平台上的任务列表
 *    - 每个任务包含 ID、英文名称和中文名称
 *
 * 2. **缺陷列表查询**
 *    - 根据任务、维度、规则等条件获取缺陷列表
 *    - 支持分页查询和多维度过滤
 *    - 返回缺陷详情和统计摘要
 *
 * **使用示例：**
 * ```kotlin
 * val codeccService = CodeCCService.getInstance(project)
 *
 * // 获取任务列表
 * when (val result = codeccService.getRemoteTaskList()) {
 *     is RemoteTaskListResult.Success -> {
 *         result.tasks.forEach { task ->
 *             println("任务: ${task.getDisplayName()} (ID: ${task.taskId})")
 *         }
 *     }
 *     is RemoteTaskListResult.Failure -> {
 *         println("获取失败: ${result.message}")
 *     }
 * }
 *
 * // 获取缺陷列表
 * when (val result = codeccService.getRemoteDefectList(
 *     taskId = 100001,
 *     dimensionList = listOf("DEFECT"),
 *     checker = "errcheck"
 * )) {
 *     is RemoteDefectListResult.Success -> {
 *         println(result.response.getSummaryText())
 *     }
 *     is RemoteDefectListResult.Failure -> {
 *         println("获取失败: ${result.message}")
 *     }
 * }
 * ```
 *
 * **线程安全性：**
 * - 所有公共方法都是挂起函数，支持协程
 * - 也提供同步阻塞版本供非协程环境使用
 *
 * @since 1.0
 */
@Service(Service.Level.PROJECT)
interface CodeCCService {

    /**
     * 获取远程任务列表
     *
     * 调用 PreCI Local Server 的 `GET /codecc/task/list` 接口获取 CodeCC 平台的任务列表。
     *
     * **注意事项：**
     * - 需要先完成 OAuth 认证（由 Local Server 自动管理）
     * - 需要先绑定蓝盾项目
     *
     * @return 任务列表查询结果
     *
     * @since 1.0
     */
    suspend fun getRemoteTaskList(): RemoteTaskListResult

    /**
     * 获取远程任务列表（同步阻塞版本）
     *
     * 与 [getRemoteTaskList] 功能相同，但使用同步阻塞方式调用。
     *
     * @return 任务列表查询结果
     *
     * @since 1.0
     */
    fun getRemoteTaskListBlocking(): RemoteTaskListResult

    /**
     * 获取远程缺陷列表
     *
     * 调用 PreCI Local Server 的 `POST /codecc/defect/list` 接口获取 CodeCC 平台的缺陷列表。
     * 支持通过任务 ID、维度、规则等条件进行过滤。
     *
     * **过滤参数说明：**
     * - `taskId`: 指定任务 ID，必须传入以获取该任务下的缺陷
     * - `dimensionList`: 维度过滤（如代码缺陷、代码规范等）
     * - `checker`: 按检查规则名称过滤
     * - `severity`: 按严重级别过滤
     * - `status`: 按缺陷状态过滤，默认只查询待修复的缺陷
     *
     * @param taskId 任务 ID
     * @param dimensionList 维度列表（可选）
     * @param checker 检查规则名称（可选）
     * @param severity 严重级别列表（可选）
     * @param status 缺陷状态列表（可选）
     * @param pageNum 页码（可选，默认 1）
     * @param pageSize 每页数量（可选，默认 100）
     * @return 缺陷列表查询结果
     *
     * @since 1.0
     */
    suspend fun getRemoteDefectList(
        taskId: Long,
        dimensionList: List<String>? = null,
        checker: String? = null,
        severity: List<String>? = null,
        status: List<String>? = null,
        pageNum: Int? = null,
        pageSize: Int? = null
    ): RemoteDefectListResult

    /**
     * 获取远程缺陷列表（同步阻塞版本）
     *
     * 与 [getRemoteDefectList] 功能相同，但使用同步阻塞方式调用。
     *
     * @param taskId 任务 ID
     * @param dimensionList 维度列表（可选）
     * @param checker 检查规则名称（可选）
     * @param severity 严重级别列表（可选）
     * @param status 缺陷状态列表（可选）
     * @param pageNum 页码（可选，默认 1）
     * @param pageSize 每页数量（可选，默认 100）
     * @return 缺陷列表查询结果
     *
     * @since 1.0
     */
    fun getRemoteDefectListBlocking(
        taskId: Long,
        dimensionList: List<String>? = null,
        checker: String? = null,
        severity: List<String>? = null,
        status: List<String>? = null,
        pageNum: Int? = null,
        pageSize: Int? = null
    ): RemoteDefectListResult

    companion object {
        /**
         * 获取 CodeCCService 实例
         *
         * @param project 当前项目
         * @return CodeCCService 实例
         */
        @JvmStatic
        fun getInstance(project: Project): CodeCCService {
            return project.getService(CodeCCService::class.java)
        }
    }
}
