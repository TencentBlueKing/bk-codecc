package com.codecc.preci.service.server

import com.codecc.preci.api.client.PreCIApiClient
import com.codecc.preci.core.http.ServerNotRunningException
import com.codecc.preci.core.http.ServerNotInstalledException
import com.codecc.preci.core.http.ServerStartFailedException
import com.codecc.preci.core.log.PreCILogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay

/**
 * 带自动服务启动的 PreCI API 客户端包装器
 *
 * 此类包装 [PreCIApiClient]，在 API 调用失败时自动检测服务状态并尝试启动服务。
 *
 * **工作流程：**
 * 1. 尝试执行 API 调用
 * 2. 如果捕获到 [ServerNotRunningException]，检查 PreCI 是否安装
 * 3. 如果已安装，尝试启动服务
 * 4. 启动成功后重试 API 调用一次
 * 5. 如果仍然失败或 PreCI 未安装，抛出相应异常
 *
 * **使用示例：**
 * ```kotlin
 * val client = AutoStartApiClient(project)
 *
 * // API 调用失败时会自动启动服务并重试
 * val response = client.executeWithAutoStart {
 *     apiClient.login(LoginRequest("pin:token", "projectId"))
 * }
 * ```
 *
 * @property project 当前 IntelliJ 项目
 * @since 1.0
 */
class AutoStartApiClient(private val project: Project) {

    private val logger = PreCILogger.getLogger(AutoStartApiClient::class.java)

    /**
     * PreCI API 客户端
     */
    val apiClient = PreCIApiClient()

    /**
     * 服务管理服务
     */
    private val serverManagement: ServerManagementService
        get() = ServerManagementService.getInstance(project)

    /**
     * 执行 API 调用，失败时自动启动服务并重试
     *
     * **执行流程：**
     * 1. 第一次尝试执行 API 调用
     * 2. 如果捕获到 [ServerNotRunningException]：
     *    a. 检查 PreCI 是否已安装
     *    b. 如果未安装，抛出 [ServerNotInstalledException]
     *    c. 如果已安装，尝试启动服务
     *    d. 启动成功后，等待 1 秒，重试 API 调用
     *    e. 启动失败，抛出 [ServerStartFailedException]
     * 3. 如果重试仍然失败，抛出原始异常
     *
     * **注意事项：**
     * - 此方法只会重试一次
     * - 仅在捕获到 [ServerNotRunningException] 时才会尝试启动服务
     * - 其他异常会直接向上抛出
     *
     * @param T API 调用的返回类型
     * @param apiCall API 调用的 lambda 表达式
     * @return API 调用的结果
     * @throws ServerNotInstalledException 如果 PreCI 未安装
     * @throws ServerStartFailedException 如果服务启动失败
     * @throws Exception 其他 API 调用异常
     */
    suspend fun <T> executeWithAutoStart(apiCall: suspend (PreCIApiClient) -> T): T {
        return try {
            // 第一次尝试
            logger.debug("Executing API call (first attempt)")
            apiCall(apiClient)
        } catch (e: ServerNotRunningException) {
            // 检测到服务未运行
            logger.warn("API call failed: server not running, attempting to start...")

            // 检查 PreCI 是否已安装
            if (!serverManagement.isPreCIInstalled()) {
                logger.error("PreCI is not installed")
                throw ServerNotInstalledException(
                    "PreCI CLI 未安装。\n" +
                    "请先安装 PreCI CLI 工具。\n" +
                    "安装指南：https://preci-docs.example.com/installation",
                    e
                )
            }

            // 尝试启动服务
            logger.info("PreCI is installed, attempting to start server...")
            val startResult = serverManagement.startServer()

            when (startResult) {
                is ServerStartResult.Success -> {
                    // 服务启动成功，等待 1 秒后重试 API 调用
                    logger.info("Server started successfully (port: ${startResult.port}), retrying API call...")
                    delay(1000) // 等待服务完全就绪

                    try {
                        apiCall(apiClient)
                    } catch (retryException: Exception) {
                        logger.error("API call failed after server start", retryException)
                        throw retryException
                    }
                }
                is ServerStartResult.Failure -> {
                    // 启动失败
                    logger.error("Failed to start server: ${startResult.message}")
                    throw ServerStartFailedException(
                        "无法启动 PreCI Local Server: ${startResult.message}\n" +
                        "请尝试手动执行 'preci server start' 命令，或查看 PreCI 日志。",
                        e
                    )
                }
            }
        }
    }

}

