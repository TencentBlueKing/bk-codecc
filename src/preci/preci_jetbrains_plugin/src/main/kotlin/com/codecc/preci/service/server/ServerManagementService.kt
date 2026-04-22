package com.codecc.preci.service.server

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * PreCI Local Server 管理服务接口
 *
 * 提供 PreCI Local Server 的完整生命周期管理功能，包括：
 * - 安装检测
 * - 运行状态检查
 * - 启动/停止/重启操作
 * - 状态监控和通知
 *
 * **架构说明：**
 * 本服务采用混合监控策略：
 * 1. IDE 启动时检测一次服务状态（不自动启动）
 * 2. API 调用失败时自动检测并尝试启动服务
 * 3. 不进行持续的后台状态轮询（避免资源消耗）
 *
 * **使用示例：**
 * ```kotlin
 * val service = project.service<ServerManagementService>()
 *
 * // 检查安装状态
 * if (service.isPreCIInstalled()) {
 *     // 检查服务运行状态
 *     if (!service.isServerRunning()) {
 *         // 启动服务
 *         val result = service.startServer()
 *         when (result) {
 *             is ServerStartResult.Success -> println("服务启动成功，端口：${result.port}")
 *             is ServerStartResult.Failure -> println("服务启动失败：${result.message}")
 *         }
 *     }
 * }
 * ```
 *
 * @since 1.0
 */
@Service(Service.Level.PROJECT)
interface ServerManagementService {

    /**
     * 检查 PreCI CLI 是否已安装
     *
     * 通过执行 `preci version` 命令判断 PreCI CLI 是否在系统 PATH 中可用。
     *
     * **实现细节：**
     * - 执行 `preci version` 命令
     * - 命令退出码为 0 表示已安装
     * - 命令不存在或执行失败表示未安装
     *
     * @return `true` 表示 PreCI CLI 已安装，`false` 表示未安装
     */
    suspend fun isPreCIInstalled(): Boolean

    /**
     * 检查 PreCI Local Server 是否正在运行
     *
     * 通过执行 `preci port` 命令判断 Local Server 是否运行。
     * 如果能成功获取端口号，说明服务正在运行。
     *
     * **实现细节：**
     * - 调用 `PreCIPortDetector.getServerPort()`
     * - 成功获取端口表示服务运行中
     * - 抛出 `ServerNotRunningException` 表示服务未运行
     *
     * @return `true` 表示服务运行中，`false` 表示服务未运行
     */
    suspend fun isServerRunning(): Boolean

    /**
     * 启动 PreCI Local Server
     *
     * 执行 `preci server start` 命令启动服务，并轮询等待服务就绪（最多 15 秒）。
     *
     * **执行流程：**
     * 1. 发送 IDE 通知：正在启动服务
     * 2. 执行 `preci server start` 命令
     * 3. 轮询检测服务是否就绪（每秒检查一次，最多 15 次）
     * 4. 服务就绪后发送成功通知
     * 5. 超时或失败时返回失败结果
     *
     * **注意事项：**
     * - 此方法是挂起函数，会阻塞当前协程直到服务启动完成或超时
     * - 如果服务已在运行，将返回成功结果
     * - 如果 PreCI 未安装，将返回失败结果
     *
     * @return [ServerStartResult.Success] 表示启动成功，包含端口号；
     *         [ServerStartResult.Failure] 表示启动失败，包含错误信息
     */
    suspend fun startServer(): ServerStartResult

    /**
     * 停止 PreCI Local Server
     *
     * 调用 `GET /shutdown` 接口优雅地停止 Local Server。
     *
     * **执行流程：**
     * 1. 检查服务是否运行
     * 2. 调用 `/shutdown` 接口
     * 3. 等待 2 秒后验证服务是否已停止
     *
     * @return [ServerStopResult.Success] 表示停止成功；
     *         [ServerStopResult.Failure] 表示停止失败，包含错误信息
     */
    suspend fun stopServer(): ServerStopResult

    /**
     * 重启 PreCI Local Server
     *
     * 先停止服务，再启动服务。
     *
     * **执行流程：**
     * 1. 调用 `stopServer()`
     * 2. 如果停止成功，等待 1 秒
     * 3. 调用 `startServer()`
     *
     * @return [ServerStartResult.Success] 表示重启成功，包含端口号；
     *         [ServerStartResult.Failure] 表示重启失败，包含错误信息
     */
    suspend fun restartServer(): ServerStartResult

    /**
     * 下载并安装 PreCI CLI
     *
     * **⚠️ 注意：本功能尚未实现**
     *
     * 自动下载和安装 PreCI CLI 涉及以下复杂性：
     * - 跨平台的安装包下载机制（Windows/macOS/Linux）
     * - 自动安装脚本的执行和权限管理
     * - 下载进度的 UI 展示
     * - 安装完成后的环境变量配置验证
     *
     * **当前行为：**
     * 抛出 `NotImplementedError`，并在异常信息中提供手动安装指南链接。
     *
     * **后续计划：**
     * 此功能将在后续任务中实现，届时将支持：
     * - 调用 `/misc/downloadLatest` 接口自动下载
     * - 显示下载进度
     * - 自动执行安装脚本
     * - 验证安装结果
     *
     * @return [InstallResult.Success] 表示安装成功；
     *         [InstallResult.Timeout] 表示安装超时；
     *         [InstallResult.Failure] 表示安装失败，包含错误信息
     * @throws NotImplementedError 当前版本尚未实现此功能
     */
    suspend fun downloadAndInstall(): InstallResult

    /**
     * 获取当前服务状态
     *
     * 返回 PreCI Local Server 的当前运行状态。
     *
     * **状态定义：**
     * - [ServerState.NOT_INSTALLED] - PreCI CLI 未安装
     * - [ServerState.STOPPED] - 服务已停止
     * - [ServerState.STARTING] - 服务启动中
     * - [ServerState.RUNNING] - 服务运行中
     * - [ServerState.STOPPING] - 服务停止中
     * - [ServerState.ERROR] - 异常状态
     *
     * @return 当前服务状态
     */
    fun getServerState(): ServerState

    /**
     * 注册服务状态变更监听器
     *
     * 用于监听服务状态变化，主要用于 UI 组件实时更新显示。
     *
     * **使用场景：**
     * - 状态栏图标和文字更新
     * - 工具窗口状态面板更新
     * - 通知用户服务状态变化
     *
     * **注意事项：**
     * - 监听器回调在 EDT（Event Dispatch Thread）线程执行
     * - 避免在回调中执行耗时操作
     * - 不会自动移除监听器，需要手动管理生命周期
     *
     * @param listener 状态变更监听器
     */
    fun addStateChangeListener(listener: ServerStateChangeListener)

    /**
     * 移除服务状态变更监听器
     *
     * @param listener 要移除的监听器
     */
    fun removeStateChangeListener(listener: ServerStateChangeListener)

    companion object {
        /**
         * 获取项目级别的 ServerManagementService 实例
         *
         * @param project 当前项目
         * @return ServerManagementService 实例
         */
        fun getInstance(project: Project): ServerManagementService {
            return project.getService(ServerManagementService::class.java)
        }
    }
}

