package com.codecc.preci.service.version

import com.codecc.preci.api.client.PreCIApiClient
import com.codecc.preci.core.http.BusinessException
import com.codecc.preci.core.http.NetworkException
import com.codecc.preci.core.http.PreCIApiException
import com.codecc.preci.core.http.ServerBusyException
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.util.ShellCommandHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 版本服务实现
 *
 * 实现 [VersionService] 接口，提供版本查询、更新检查和执行更新功能。
 *
 * **实现策略：**
 * - 获取当前版本：通过 [ShellCommandHelper.createProcessBuilder] 执行 `preci version`
 * - 获取最新版本：通过 [PreCIApiClient.getLatestVersion] 调用 `GET /misc/latestVersion`
 * - 执行更新：通过 [ShellCommandHelper.createProcessBuilder] 执行 `preci update`
 *
 * @since 2.0
 */
class VersionServiceImpl : VersionService {

    private val logger = PreCILogger.getLogger(VersionServiceImpl::class.java)
    private val apiClient = PreCIApiClient()

    /** `preci version` 命令的最大等待时间（秒） */
    private val versionCommandTimeout = 10L

    /** `preci update` 命令的最大等待时间（秒），更新包含下载和替换，需要较长时间 */
    private val updateCommandTimeout = 300L

    companion object {
        private val updating = AtomicBoolean(false)

        /** updater 替换二进制后 SIGKILL 原进程的退出码 (128 + 9) */
        private const val SIGKILL_EXIT_CODE = 137
    }

    override suspend fun getCurrentVersion(): VersionResult = withContext(Dispatchers.IO) {
        try {
            logger.info("开始获取当前 PreCI CLI 版本")
            val version = executeVersionCommand()
            logger.info("成功获取当前版本: $version")
            VersionResult.Success(version)
        } catch (e: Exception) {
            logger.error("获取当前版本失败: ${e.message}", e)
            VersionResult.Failure("获取当前版本失败: ${e.message}", e)
        }
    }

    override suspend fun getLatestVersion(): VersionResult = withContext(Dispatchers.IO) {
        try {
            logger.info("开始获取线上最新版本")
            val response = apiClient.getLatestVersion()
            val latestVersion = response.latestVersion.trim()
            logger.info("成功获取最新版本: $latestVersion")
            VersionResult.Success(latestVersion)
        } catch (e: BusinessException) {
            logger.error("获取最新版本失败 (业务异常): ${e.message}", e)
            VersionResult.Failure("获取最新版本失败: ${e.message}", e)
        } catch (e: NetworkException) {
            logger.error("获取最新版本网络错误: ${e.message}", e)
            val msg = if (e is ServerBusyException) "服务响应超时：${e.message}" else "网络错误，请检查 Local Server 是否运行: ${e.message}"
            VersionResult.Failure(msg, e)
        } catch (e: PreCIApiException) {
            logger.error("获取最新版本 API 错误: ${e.message}", e)
            VersionResult.Failure("服务错误: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("获取最新版本异常: ${e.message}", e)
            VersionResult.Failure("获取最新版本失败: ${e.message}", e)
        }
    }

    override suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        logger.info("开始检查更新")

        val currentResult = getCurrentVersion()
        if (currentResult is VersionResult.Failure) {
            return@withContext UpdateCheckResult.Failure(
                "无法获取当前版本: ${currentResult.message}",
                currentResult.exception
            )
        }
        val currentVersion = (currentResult as VersionResult.Success).version

        val latestResult = getLatestVersion()
        if (latestResult is VersionResult.Failure) {
            return@withContext UpdateCheckResult.Failure(
                "无法获取最新版本: ${latestResult.message}",
                latestResult.exception
            )
        }
        val latestVersion = (latestResult as VersionResult.Success).version

        val needsUpdate = normalizeVersion(currentVersion) != normalizeVersion(latestVersion)
        if (needsUpdate) {
            logger.info("发现新版本: $latestVersion（当前: $currentVersion）")
            UpdateCheckResult.UpdateAvailable(currentVersion, latestVersion)
        } else {
            logger.info("已是最新版本: $currentVersion")
            UpdateCheckResult.AlreadyLatest(currentVersion)
        }
    }

    override suspend fun performUpdate(): UpdateResult = withContext(Dispatchers.IO) {
        if (!updating.compareAndSet(false, true)) {
            logger.info("已有更新任务在进行中，跳过本次请求")
            return@withContext UpdateResult.Failure("已有更新任务在进行中")
        }

        try {
            logger.info("开始执行 PreCI CLI 更新")
            val output = executeUpdateCommand()
            logger.info("更新命令执行完成: $output")
            UpdateResult.Success(output)
        } catch (e: Exception) {
            logger.error("执行更新失败: ${e.message}", e)
            UpdateResult.Failure("更新失败: ${e.message}", e)
        } finally {
            updating.set(false)
        }
    }

    /**
     * 执行 `preci version` 命令并返回版本号
     *
     * CLI 输出格式示例：
     * ```
     * 2026/03/02 21:33:35 port: 51126, install dir: /Users/user/PreCI
     * v0.0.69
     * ```
     * 版本号在最后一行，前面的行是日志信息。
     *
     * @return 版本号字符串（已 trim）
     * @throws RuntimeException 命令执行失败或超时
     */
    internal fun executeVersionCommand(): String {
        val processBuilder = ShellCommandHelper.createProcessBuilder("preci", "version")
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText().trim()
        val completed = process.waitFor(versionCommandTimeout, TimeUnit.SECONDS)

        if (!completed) {
            process.destroyForcibly()
            throw RuntimeException("preci version 命令执行超时（${versionCommandTimeout}s）")
        }

        val exitCode = process.exitValue()
        if (exitCode != 0) {
            throw RuntimeException("preci version 命令执行失败（exitCode=$exitCode）: $output")
        }

        if (output.isBlank()) {
            throw RuntimeException("preci version 命令输出为空")
        }

        return output.lines().last { it.isNotBlank() }.trim()
    }

    /**
     * 执行 `preci update` 命令并返回输出
     *
     * 更新流程中，updater 会替换 preci 二进制并终止原进程，
     * 导致 exit code 为 137（SIGKILL）。这属于正常行为，不应视为失败。
     *
     * @return 命令输出字符串
     * @throws RuntimeException 命令执行失败或超时
     */
    internal fun executeUpdateCommand(): String {
        val processBuilder = ShellCommandHelper.createProcessBuilder("preci", "update")
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText().trim()
        val completed = process.waitFor(updateCommandTimeout, TimeUnit.SECONDS)

        if (!completed) {
            process.destroyForcibly()
            throw RuntimeException("preci update 命令执行超时（${updateCommandTimeout}s）")
        }

        val exitCode = process.exitValue()
        if (exitCode == SIGKILL_EXIT_CODE) {
            logger.info("preci update 进程被 SIGKILL 终止（exitCode=$SIGKILL_EXIT_CODE），属于 updater 替换二进制的正常行为")
            return output
        }
        if (exitCode != 0) {
            throw RuntimeException("preci update 命令执行失败（exitCode=$exitCode）: $output")
        }

        return output
    }

    /**
     * 规范化版本号用于比较
     *
     * 去除前缀 "v" 并 trim 空白字符，使 "v1.0.0" 和 "1.0.0" 可以正确比较。
     *
     * @param version 原始版本号
     * @return 规范化后的版本号
     */
    internal fun normalizeVersion(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V")
    }
}
