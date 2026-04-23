package com.codecc.preci.service.pipeline

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * PAC 流水线服务接口
 *
 * 负责与蓝盾流水线交互，提供构建触发、历史查询、日志查看和构建停止等功能。
 * 通过 PreCI Local Server 代理访问蓝盾 BK-CI 网关。
 *
 * **核心功能：**
 *
 * 1. **构建历史查询**
 *    - 获取当前项目的流水线构建历史记录
 *    - 支持分页查询
 *
 * 2. **触发构建**
 *    - 触发一次新的流水线构建
 *
 * 3. **构建日志查看**
 *    - 获取指定构建的日志内容
 *    - 支持增量获取（通过 start 参数）
 *
 * 4. **构建详情查询**
 *    - 获取指定构建的详细信息
 *
 * 5. **停止构建**
 *    - 停止正在运行的构建
 *
 * **使用示例：**
 * ```kotlin
 * val pipelineService = PipelineService.getInstance(project)
 *
 * // 获取构建历史
 * when (val result = pipelineService.getBuildHistory()) {
 *     is BuildHistoryResult.Success -> {
 *         result.builds.forEach { build ->
 *             println("构建: #${build.buildNum} - ${build.status}")
 *         }
 *     }
 *     is BuildHistoryResult.Failure -> {
 *         println("获取失败: ${result.message}")
 *     }
 * }
 * ```
 *
 * **线程安全性：**
 * - 所有公共方法都是挂起函数，支持协程
 *
 * @since 1.0
 */
@Service(Service.Level.PROJECT)
interface PipelineService {

    /**
     * 获取构建历史
     *
     * 调用 PreCI Local Server 的 `GET /pipeline/build/history` 接口获取构建列表。
     *
     * @return 构建历史查询结果
     *
     * @since 1.0
     */
    suspend fun getBuildHistory(): BuildHistoryResult

    /**
     * 触发新构建
     *
     * 调用 PreCI Local Server 的 `POST /pipeline/build/start` 接口触发流水线构建。
     *
     * @return 触发构建结果，成功时包含新构建的 buildId
     *
     * @since 1.0
     */
    suspend fun startBuild(): StartBuildResult

    /**
     * 获取构建日志
     *
     * 调用 PreCI Local Server 的 `GET /pipeline/build/{buildId}/logs` 接口获取日志。
     * 支持增量获取：通过 start 参数指定起始行号，仅返回该行号之后的日志。
     *
     * @param buildId 构建唯一标识 ID
     * @param start 起始行号，默认为 0（从头开始）
     * @return 构建日志查询结果
     *
     * @since 1.0
     */
    suspend fun getBuildLogs(buildId: String, start: Long = 0): BuildLogsResult

    /**
     * 获取构建详情
     *
     * 调用 PreCI Local Server 的 `GET /pipeline/build/{buildId}/detail` 接口获取详情。
     *
     * @param buildId 构建唯一标识 ID
     * @return 构建详情查询结果
     *
     * @since 1.0
     */
    suspend fun getBuildDetail(buildId: String): BuildDetailResult

    /**
     * 停止构建
     *
     * 调用 PreCI Local Server 的 `DELETE /pipeline/build/{buildId}/stop` 接口停止构建。
     *
     * @param buildId 构建唯一标识 ID
     * @return 停止构建结果
     *
     * @since 1.0
     */
    suspend fun stopBuild(buildId: String): StopBuildResult

    companion object {
        /**
         * 获取 PipelineService 实例
         *
         * @param project 当前项目
         * @return PipelineService 实例
         */
        @JvmStatic
        fun getInstance(project: Project): PipelineService {
            return project.getService(PipelineService::class.java)
        }
    }
}
