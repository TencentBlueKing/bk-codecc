package com.codecc.preci.service.version

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

/**
 * 版本服务接口
 *
 * 负责 PreCI CLI 的版本查询、更新检查和执行更新操作。
 * 本接口封装了 CLI 命令（`preci version`、`preci update`）
 * 和 Local Server API（`GET /misc/latestVersion`）的调用。
 *
 * **核心功能：**
 *
 * 1. **获取当前版本**
 *    - 执行 `preci version` 命令获取本地安装的 CLI 版本
 *
 * 2. **获取最新版本**
 *    - 调用 `GET /misc/latestVersion` 接口获取线上最新版本
 *
 * 3. **检查更新**
 *    - 比较本地版本和线上版本，判断是否有可用更新
 *
 * 4. **执行更新**
 *    - 执行 `preci update` 命令完成更新（包含下载、解压、替换、重启服务）
 *
 * **使用示例：**
 * ```kotlin
 * val versionService = VersionService.getInstance()
 *
 * // 检查更新
 * when (val result = versionService.checkForUpdate()) {
 *     is UpdateCheckResult.UpdateAvailable -> {
 *         println("发现新版本: ${result.latestVersion}（当前: ${result.currentVersion}）")
 *     }
 *     is UpdateCheckResult.AlreadyLatest -> {
 *         println("已是最新版本: ${result.currentVersion}")
 *     }
 *     is UpdateCheckResult.Failure -> {
 *         println("检查更新失败: ${result.message}")
 *     }
 * }
 *
 * // 执行更新
 * when (val result = versionService.performUpdate()) {
 *     is UpdateResult.Success -> println("更新成功")
 *     is UpdateResult.Failure -> println("更新失败: ${result.message}")
 * }
 * ```
 *
 * **线程安全性：**
 * - 所有公共方法都是挂起函数，支持协程
 * - 可以在任意协程作用域中调用
 *
 * @since 2.0
 */
@Service(Service.Level.APP)
interface VersionService {

    /**
     * 获取当前本地 PreCI CLI 版本
     *
     * 执行 `preci version` 命令并解析输出获取版本号。
     *
     * @return 版本查询结果
     *
     * @since 2.0
     */
    suspend fun getCurrentVersion(): VersionResult

    /**
     * 获取线上最新版本
     *
     * 调用 Local Server 的 `GET /misc/latestVersion` 接口获取最新版本号。
     *
     * **注意：** 需要 Local Server 处于运行状态。
     *
     * @return 版本查询结果
     *
     * @since 2.0
     */
    suspend fun getLatestVersion(): VersionResult

    /**
     * 检查是否有可用更新
     *
     * 同时获取当前版本和最新版本并进行比较。
     *
     * **检查流程：**
     * 1. 执行 `preci version` 获取本地版本
     * 2. 调用 `GET /misc/latestVersion` 获取线上版本
     * 3. 比较两个版本号，判断是否需要更新
     *
     * @return 更新检查结果
     *
     * @since 2.0
     */
    suspend fun checkForUpdate(): UpdateCheckResult

    /**
     * 执行更新
     *
     * 执行 `preci update` CLI 命令完成更新。
     * 该命令会自动完成：下载最新版本、解压、替换文件、重启 Local Server。
     *
     * **注意：**
     * - 更新过程中 Local Server 会被重启
     * - 更新完成后可能需要重新检测端口
     *
     * @return 更新执行结果
     *
     * @since 2.0
     */
    suspend fun performUpdate(): UpdateResult

    companion object {
        /**
         * 获取 VersionService 实例
         *
         * 从 IntelliJ Platform 的服务容器中获取 Application 级别的 VersionService 实例。
         *
         * @return VersionService 实例
         *
         * @since 2.0
         */
        @JvmStatic
        fun getInstance(): VersionService {
            return ApplicationManager.getApplication().getService(VersionService::class.java)
        }
    }
}
