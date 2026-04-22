package com.codecc.preci.service.server

import com.codecc.preci.api.client.PreCIApiClient
import com.codecc.preci.core.http.ServerNotRunningException
import com.codecc.preci.util.PreCIPortDetector
import com.codecc.preci.util.ShellCommandHelper
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.codecc.preci.core.log.PreCILogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * PreCI Local Server 管理服务实现
 *
 * 实现 [ServerManagementService] 接口，提供 PreCI Local Server 的完整生命周期管理。
 *
 * **实现特点：**
 * - 线程安全的状态管理
 * - 协程支持的异步操作
 * - 自动轮询等待服务就绪
 * - IDE 通知集成
 * - 状态变更监听器支持
 *
 * @property project 当前 IntelliJ 项目
 * @since 1.0
 */
class ServerManagementServiceImpl(
    private val project: Project
) : ServerManagementService {

    private val logger = PreCILogger.getLogger(ServerManagementServiceImpl::class.java)

    /**
     * 当前服务状态
     * 使用 @Volatile 保证多线程可见性
     */
    @Volatile
    private var currentState: ServerState = ServerState.STOPPED

    /**
     * 状态变更监听器列表
     * 使用 CopyOnWriteArrayList 保证线程安全
     */
    private val stateChangeListeners = CopyOnWriteArrayList<ServerStateChangeListener>()

    /**
     * PreCI API 客户端
     */
    private val apiClient = PreCIApiClient()

    /**
     * 通知分组 ID
     */
    private companion object {
        const val NOTIFICATION_GROUP_ID = "PreCI.ServerManagement"
        const val SERVER_START_TIMEOUT_SECONDS = 15
        const val SERVER_STOP_WAIT_SECONDS = 2L
        const val RESTART_DELAY_SECONDS = 1L
        const val DOWNLOAD_CONNECT_TIMEOUT_SECONDS = 30L
        const val DOWNLOAD_READ_TIMEOUT_SECONDS = 300L
    }

    override suspend fun isPreCIInstalled(): Boolean = withContext(Dispatchers.IO) {
        logger.info("Checking if PreCI is installed...")
        val detection = ShellCommandHelper.detectPreCI()
        val isReady = detection.status == ShellCommandHelper.DetectionStatus.READY
        logger.info("PreCI detection: status=${detection.status}, path=${detection.path}, ready=$isReady")
        return@withContext isReady
    }

    override suspend fun isServerRunning(): Boolean = withContext(Dispatchers.IO) {
        try {
            val port = PreCIPortDetector.getServerPort()

            // 端口文件可能是上次残留的，需要实际连接验证 server 是否存活
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress("localhost", port), 2000)
            }
            logger.info("PreCI Server is running on port $port")
            return@withContext true
        } catch (e: ServerNotRunningException) {
            logger.debug("PreCI Server is not running: ${e.message}")
            return@withContext false
        } catch (e: java.net.ConnectException) {
            logger.warn("Port file exists but server is not reachable")
            return@withContext false
        } catch (e: java.net.SocketTimeoutException) {
            logger.warn("Server port detected but connection timed out")
            return@withContext false
        } catch (e: Exception) {
            logger.warn("Error checking server status", e)
            return@withContext false
        }
    }

    override suspend fun startServer(): ServerStartResult = withContext(Dispatchers.IO) {
        logger.info("Attempting to start PreCI Server...")

        // 更新状态为启动中
        updateState(ServerState.STARTING)

        try {
            // 检查是否已在运行
            if (isServerRunning()) {
                logger.info("Server is already running")
                val port = PreCIPortDetector.getServerPort()
                updateState(ServerState.RUNNING)
                return@withContext ServerStartResult.Success(port)
            }

            // 发送通知：正在启动服务
            notifyUser("正在启动 PreCI Local Server...", NotificationType.INFORMATION)

            val process = ShellCommandHelper.createProcessBuilder("preci", "server", "start")
                .redirectErrorStream(true)
                .start()

            // Drain stdout asynchronously: on Windows, child daemon processes inherit
            // the stdout handle, causing synchronous readText() to block indefinitely.
            val outputFuture = java.util.concurrent.CompletableFuture.supplyAsync {
                try {
                    process.inputStream.bufferedReader().use { it.readText() }
                } catch (_: Exception) { "" }
            }

            val completed = process.waitFor(5, TimeUnit.SECONDS)

            if (!completed) {
                logger.info("Command 'preci server start' did not exit within timeout, proceeding to poll for readiness")
                process.destroyForcibly()
            }

            val output = try {
                outputFuture.get(2, TimeUnit.SECONDS)
            } catch (_: Exception) { "" }

            val exitCode = if (completed) process.exitValue() else -1
            logger.debug("preci server start exit code: $exitCode, output: $output")

            if (completed && exitCode != 0) {
                logger.warn("Command 'preci server start' failed with exit code $exitCode")
                updateState(ServerState.ERROR)
                notifyUser("启动 PreCI Server 失败：$output", NotificationType.ERROR)
                return@withContext ServerStartResult.Failure("启动命令失败：exit code $exitCode")
            }

            // 轮询检测服务是否就绪（最多 15 秒，每秒检查一次）
            logger.info("Waiting for server to be ready...")
            repeat(SERVER_START_TIMEOUT_SECONDS) { attempt ->
                delay(1000) // 等待 1 秒

                if (isServerRunning()) {
                    val port = PreCIPortDetector.getServerPort()
                    logger.info("Server started successfully on port $port after ${attempt + 1} seconds")
                    updateState(ServerState.RUNNING)
                    notifyUser("PreCI Local Server 启动成功（端口：$port）", NotificationType.INFORMATION)
                    return@withContext ServerStartResult.Success(port)
                }

                logger.debug("Server not ready yet, attempt ${attempt + 1}/$SERVER_START_TIMEOUT_SECONDS")
            }

            // 超时
            logger.warn("Server start timeout after $SERVER_START_TIMEOUT_SECONDS seconds")
            updateState(ServerState.ERROR)
            notifyUser(
                "启动 PreCI Server 超时，请检查服务是否正常启动",
                NotificationType.WARNING
            )
            return@withContext ServerStartResult.Failure("服务启动超时（${SERVER_START_TIMEOUT_SECONDS}秒）")

        } catch (e: IOException) {
            logger.error("IO error starting server", e)
            updateState(ServerState.ERROR)
            notifyUser("启动 PreCI Server 失败：${e.message}", NotificationType.ERROR)
            return@withContext ServerStartResult.Failure("IO 错误：${e.message}")
        } catch (e: Exception) {
            logger.error("Unexpected error starting server", e)
            updateState(ServerState.ERROR)
            notifyUser("启动 PreCI Server 失败：${e.message}", NotificationType.ERROR)
            return@withContext ServerStartResult.Failure("未知错误：${e.message}")
        }
    }

    override suspend fun stopServer(): ServerStopResult = withContext(Dispatchers.IO) {
        logger.info("Attempting to stop PreCI Server...")

        // 更新状态为停止中
        updateState(ServerState.STOPPING)

        try {
            // 检查服务是否运行
            if (!isServerRunning()) {
                logger.info("Server is not running, no need to stop")
                updateState(ServerState.STOPPED)
                return@withContext ServerStopResult.Success
            }

            // 调用 /shutdown 接口
            apiClient.shutdown()
            logger.info("Shutdown request sent")

            // 等待 2 秒后验证服务是否已停止
            delay(SERVER_STOP_WAIT_SECONDS * 1000)

            if (!isServerRunning()) {
                logger.info("Server stopped successfully")
                updateState(ServerState.STOPPED)
                notifyUser("PreCI Local Server 已停止", NotificationType.INFORMATION)
                return@withContext ServerStopResult.Success
            } else {
                logger.warn("Server did not stop after shutdown request")
                updateState(ServerState.ERROR)
                notifyUser(
                    "停止 PreCI Server 失败，服务仍在运行",
                    NotificationType.WARNING
                )
                return@withContext ServerStopResult.Failure("服务未能正常停止")
            }

        } catch (e: ServerNotRunningException) {
            // 服务本来就不在运行
            logger.info("Server was not running: ${e.message}")
            updateState(ServerState.STOPPED)
            return@withContext ServerStopResult.Success
        } catch (e: Exception) {
            logger.error("Error stopping server", e)
            updateState(ServerState.ERROR)
            notifyUser("停止 PreCI Server 失败：${e.message}", NotificationType.ERROR)
            return@withContext ServerStopResult.Failure("错误：${e.message}")
        }
    }

    override suspend fun restartServer(): ServerStartResult {
        logger.info("Attempting to restart PreCI Server...")

        // 先停止服务
        val stopResult = stopServer()

        when (stopResult) {
            is ServerStopResult.Success -> {
                // 停止成功，等待 1 秒后启动
                delay(RESTART_DELAY_SECONDS * 1000)
                return startServer()
            }
            is ServerStopResult.Failure -> {
                logger.warn("Failed to stop server during restart: ${stopResult.message}")
                notifyUser(
                    "重启失败：无法停止服务（${stopResult.message}）",
                    NotificationType.ERROR
                )
                return ServerStartResult.Failure("无法停止服务：${stopResult.message}")
            }
        }
    }

    override suspend fun downloadAndInstall(): InstallResult = withContext(Dispatchers.IO) {
        logger.info("Starting PreCI download...")

        try {
            val packageInfo = PreCIPackageInfo.detect()
            logger.info("Detected OS package: ${packageInfo.fileName} (${packageInfo.displayName})")

            val installDir = Path.of(System.getProperty("user.home"), ".preci", "install-temp")
            // 清理上一次的残留文件
            if (Files.exists(installDir)) {
                installDir.toFile().deleteRecursively()
            }
            Files.createDirectories(installDir)

            val zipFile = installDir.resolve(packageInfo.fileName)

            notifyUser("正在下载 PreCI (${packageInfo.displayName})...", NotificationType.INFORMATION)
            downloadFile(packageInfo.downloadUrl, zipFile)
            logger.info("Download completed: $zipFile")

            notifyUser("正在解压 PreCI...", NotificationType.INFORMATION)
            extractZip(zipFile, installDir)
            logger.info("Extraction completed to: $installDir")

            // 删除 zip 包，只保留解压后的文件
            Files.deleteIfExists(zipFile)

            val scriptName = if (packageInfo.isWindows) "install.ps1" else "install.sh"
            val scriptPath = findInstallScript(installDir, scriptName)
                ?: return@withContext InstallResult.Failure("解压完成但未找到安装脚本 $scriptName")

            if (!packageInfo.isWindows) {
                ProcessBuilder("chmod", "+x", scriptPath.toString())
                    .start()
                    .waitFor(5, TimeUnit.SECONDS)
            }

            logger.info("Ready for installation, script: $scriptPath")
            return@withContext InstallResult.Success(scriptPath.toAbsolutePath().toString())
        } catch (e: Exception) {
            logger.error("Failed to download PreCI", e)
            notifyUser("PreCI 下载失败：${e.message}", NotificationType.ERROR)
            return@withContext InstallResult.Failure(e.message)
        }
    }

    /**
     * 从指定 URL 下载文件到本地路径
     */
    private fun downloadFile(url: String, destination: Path) {
        val client = OkHttpClient.Builder()
            .connectTimeout(DOWNLOAD_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(DOWNLOAD_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("下载失败: HTTP ${response.code}")
            }

            response.body?.byteStream()?.use { input ->
                Files.newOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("下载响应体为空")
        }
    }

    /**
     * 解压 ZIP 文件到目标目录，包含 zip-slip 安全防护
     */
    private fun extractZip(zipFile: Path, targetDir: Path) {
        ZipInputStream(Files.newInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryPath = targetDir.resolve(entry.name).normalize()
                if (!entryPath.startsWith(targetDir)) {
                    throw IOException("Zip entry outside of target directory: ${entry.name}")
                }
                if (entry.isDirectory) {
                    Files.createDirectories(entryPath)
                } else {
                    Files.createDirectories(entryPath.parent)
                    Files.newOutputStream(entryPath).use { output ->
                        zis.copyTo(output)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    /**
     * 在目录及其一级子目录中查找安装脚本
     */
    private fun findInstallScript(dir: Path, scriptName: String): Path? {
        val directPath = dir.resolve(scriptName)
        if (Files.exists(directPath)) return directPath

        return Files.list(dir).use { stream ->
            stream
                .filter { Files.isDirectory(it) }
                .map { it.resolve(scriptName) }
                .filter { Files.exists(it) }
                .findFirst()
                .orElse(null)
        }
    }

    override fun getServerState(): ServerState {
        return currentState
    }

    override fun addStateChangeListener(listener: ServerStateChangeListener) {
        stateChangeListeners.add(listener)
        logger.debug("Added state change listener: $listener")
    }

    override fun removeStateChangeListener(listener: ServerStateChangeListener) {
        stateChangeListeners.remove(listener)
        logger.debug("Removed state change listener: $listener")
    }

    /**
     * 更新服务状态并通知监听器
     *
     * @param newState 新状态
     */
    private fun updateState(newState: ServerState) {
        val oldState = currentState
        if (oldState != newState) {
            currentState = newState
            logger.info("Server state changed: $oldState -> $newState")

            // 通知所有监听器
            stateChangeListeners.forEach { listener ->
                try {
                    listener.onStateChanged(oldState, newState)
                } catch (e: Exception) {
                    logger.error("Error notifying state change listener", e)
                }
            }
        }
    }

    /**
     * 发送 IDE 通知
     *
     * @param content 通知内容
     * @param type 通知类型
     */
    private fun notifyUser(content: String, type: NotificationType) {
        val notification = Notification(
            NOTIFICATION_GROUP_ID,
            "PreCI Server",
            content,
            type
        )
        Notifications.Bus.notify(notification, project)
        logger.debug("Notification sent: $content (type: $type)")
    }
}

