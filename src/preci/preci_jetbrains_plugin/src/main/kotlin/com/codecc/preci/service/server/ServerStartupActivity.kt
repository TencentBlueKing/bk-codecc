package com.codecc.preci.service.server

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.codecc.preci.core.log.LogbackConfigurator
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.auth.AuthService
import com.codecc.preci.service.auth.ProjectListResult
import com.codecc.preci.ui.annotator.PreCIDefectHighlightListener
import com.codecc.preci.ui.settings.PreCIConfigurable
import com.codecc.preci.util.ShellCommandHelper
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.io.File

/**
 * PreCI Server 启动检查活动
 *
 * 在 IDE 项目打开时自动执行，检查 PreCI CLI 的安装状态和 Local Server 的运行状态。
 *
 * **检查流程：**
 * 1. 检查 PreCI CLI 是否已安装
 * 2. 如果未安装，显示通知提示用户安装
 * 3. 如果已安装，检查 Local Server 是否运行
 * 4. 仅更新内部状态，不自动启动服务（遵循懒加载策略）
 *
 * **设计原则：**
 * - 非侵入式：不自动启动服务，避免打扰用户
 * - 信息透明：明确告知用户当前状态
 * - 提供引导：未安装时提供安装指引
 *
 * **使用方式：**
 * 在 `plugin.xml` 中注册为 `postStartupActivity`：
 * ```xml
 * <postStartupActivity implementation="com.codecc.preci.service.server.ServerStartupActivity"/>
 * ```
 *
 * @since 1.0
 */
class ServerStartupActivity : ProjectActivity {

    private val logger = PreCILogger.getLogger(ServerStartupActivity::class.java)

    companion object {
        const val NOTIFICATION_GROUP_ID = "PreCI.ServerManagement"
        const val NOTIFICATION_GROUP_ID_INSTALL = "PreCI.Install"
        const val DOWNLOAD_BASE_URL =
            "https://bkrepo.woa.com/generic/bkdevops/static/gw/resource/preci/v2/latest/"

        @Volatile
        private var logInitialized = false

        /**
         * 全局服务就绪信号。
         * 其他组件（如设置面板）可 await 此 Deferred，确保在服务就绪后再发起 API 请求。
         * - true: 服务已就绪
         * - false: 服务启动失败或 CLI 未安装
         */
        val serverReady: CompletableDeferred<Boolean> = CompletableDeferred()

        /**
         * 等待服务就绪，带超时。
         * @param timeoutMs 超时时间（毫秒），默认 30 秒
         * @return true 如果服务就绪，false 如果超时或失败
         */
        suspend fun awaitServerReady(timeoutMs: Long = 30_000L): Boolean {
            return withTimeoutOrNull(timeoutMs) { serverReady.await() } ?: false
        }
    }

    /**
     * 项目启动时执行
     *
     * @param project 当前打开的项目
     */
    override suspend fun execute(project: Project) {
        // 初始化 Logback 配置（只初始化一次）
        if (!logInitialized) {
            synchronized(this) {
                if (!logInitialized) {
                    try {
                        LogbackConfigurator.initialize()
                        logInitialized = true
                    } catch (e: Exception) {
                        System.err.println("初始化日志配置失败: ${e.message}")
                    }
                }
            }
        }

        logger.info("PreCI Server startup check started for project: ${project.name}")

        // 创建项目级协程作用域
        val projectCoroutineScope = CoroutineScope(Dispatchers.IO)

        // 注册告警高亮监听器
        try {
            PreCIDefectHighlightListener.register(project, projectCoroutineScope)
            logger.info("PreCI defect highlight listener registered for project: ${project.name}")
        } catch (e: Exception) {
            logger.error("Failed to register defect highlight listener", e)
        }

        // 在 IO 协程中执行检查和自动启动
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. 检测 PreCI CLI 安装状态和版本
                val detection = ShellCommandHelper.detectPreCI()
                logger.info("PreCI detection: status=${detection.status}, path=${detection.path}")

                when (detection.status) {
                    ShellCommandHelper.DetectionStatus.NOT_FOUND -> {
                        showNotInstalledNotification(project)
                        serverReady.complete(false)
                        return@launch
                    }
                    ShellCommandHelper.DetectionStatus.OLD_VERSION -> {
                        showOldVersionNotification(project, detection)
                        serverReady.complete(false)
                        return@launch
                    }
                    ShellCommandHelper.DetectionStatus.READY -> {
                        logger.info("PreCI CLI v2 detected at: ${detection.path}")
                    }
                }

                // 2. 检查服务运行状态，未运行则主动启动
                val serverManagement = ServerManagementService.getInstance(project)
                val isRunning = serverManagement.isServerRunning()
                logger.info("PreCI Server running status: $isRunning")

                if (isRunning) {
                    logger.info("PreCI Server is already running")
                    serverReady.complete(true)
                    checkLoginStatus(project)
                } else {
                    logger.info("PreCI Server is not running, auto-starting...")
                    val result = serverManagement.startServer()
                    when (result) {
                        is ServerStartResult.Success -> {
                            logger.info("PreCI Server auto-started on port ${result.port}")
                            serverReady.complete(true)
                            checkLoginStatus(project)
                        }
                        is ServerStartResult.Failure -> {
                            logger.warn("PreCI Server auto-start failed: ${result.message}")
                            serverReady.complete(false)
                        }
                    }
                }

            } catch (e: Exception) {
                logger.error("Error during PreCI Server startup check", e)
                if (!serverReady.isCompleted) {
                    serverReady.complete(false)
                }
            }
        }
    }

    /**
     * 检测登录态，未登录时通知用户授权
     */
    private suspend fun checkLoginStatus(project: Project) {
        try {
            val authService = AuthService.getInstance()
            val result = authService.getProjects()
            if (result is ProjectListResult.Failure) {
                logger.info("Not logged in, prompting OAuth authorization")
                showLoginRequiredNotification(project)
            }
        } catch (e: Exception) {
            logger.info("Login status check failed, prompting authorization: ${e.message}")
            showLoginRequiredNotification(project)
        }
    }

    private fun showLoginRequiredNotification(project: Project) {
        val notification = Notification(
            NOTIFICATION_GROUP_ID,
            "PreCI 需要登录",
            "请登录以使用 PreCI 代码扫描功能。",
            NotificationType.WARNING
        )

        notification.addAction(
            NotificationAction.createSimple("登录") {
                notification.expire()
                CoroutineScope(Dispatchers.IO).launch {
                    val authService = AuthService.getInstance()
                    authService.loginWithOAuth()
                }
            }
        )

        Notifications.Bus.notify(notification, project)
    }

    /**
     * 显示 PreCI 未安装的通知
     */
    private fun showNotInstalledNotification(project: Project) {
        val notification = Notification(
            NOTIFICATION_GROUP_ID,
            "PreCI 未安装",
            "未检测到 PreCI CLI。要使用代码扫描功能，请先安装 PreCI CLI。" +
                "\n下载地址：$DOWNLOAD_BASE_URL",
            NotificationType.WARNING
        )

        notification.addAction(
            NotificationAction.createSimple("自动安装") {
                handleAutoInstall(project, notification)
            }
        )

        notification.addAction(
            NotificationAction.createSimple("手动下载") {
                BrowserUtil.browse(DOWNLOAD_BASE_URL)
                notification.expire()
            }
        )

        notification.addAction(
            NotificationAction.createSimple("配置路径") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, PreCIConfigurable::class.java)
                notification.expire()
            }
        )

        Notifications.Bus.notify(notification, project)
        logger.info("PreCI not installed notification shown")
    }

    /**
     * 显示 PreCI 旧版本的通知
     */
    private fun showOldVersionNotification(
        project: Project,
        detection: ShellCommandHelper.DetectionResult
    ) {
        val notification = Notification(
            NOTIFICATION_GROUP_ID,
            "PreCI 版本过旧",
            "检测到旧版本的 PreCI CLI，需要升级到 v2 才能使用代码扫描功能。\n" +
                "当前路径：${detection.path ?: "未知"}\n" +
                "版本信息：${detection.message ?: "无"}",
            NotificationType.WARNING
        )

        notification.addAction(
            NotificationAction.createSimple("自动安装新版本") {
                handleAutoInstall(project, notification)
            }
        )

        notification.addAction(
            NotificationAction.createSimple("手动下载") {
                BrowserUtil.browse(DOWNLOAD_BASE_URL)
                notification.expire()
            }
        )

        notification.addAction(
            NotificationAction.createSimple("配置路径") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, PreCIConfigurable::class.java)
                notification.expire()
            }
        )

        Notifications.Bus.notify(notification, project)
        logger.info("PreCI old version notification shown: ${detection.message}")
    }

    /**
     * 处理自动安装操作
     *
     * 弹出确认对话框（显示系统信息和即将下载的包），用户确认后在后台执行下载安装。
     */
    private fun handleAutoInstall(project: Project, notification: Notification) {
        try {
            val packageInfo = PreCIPackageInfo.detect()

            ApplicationManager.getApplication().invokeLater {
                val confirmed = Messages.showOkCancelDialog(
                    project,
                    "检测到您的系统为 ${packageInfo.displayName}\n\n" +
                        "将下载 ${packageInfo.fileName} 并自动安装。\n" +
                        "下载地址：${packageInfo.downloadUrl}\n\n" +
                        "是否继续？",
                    "安装 PreCI",
                    "确认安装",
                    "取消",
                    Messages.getQuestionIcon()
                )

                if (confirmed == Messages.OK) {
                    notification.expire()
                    logger.info("User confirmed auto-install for ${packageInfo.displayName}")

                    CoroutineScope(Dispatchers.IO).launch {
                        val serverManagement = ServerManagementService.getInstance(project)
                        val result = serverManagement.downloadAndInstall()
                        when (result) {
                            is InstallResult.Success -> {
                                logger.info("Download completed, script: ${result.installScriptPath}")
                                openTerminalAndRunInstall(project, result.installScriptPath)
                            }
                            is InstallResult.Failure ->
                                logger.warn("Auto-install failed: ${result.message}")
                            is InstallResult.Timeout ->
                                logger.warn("Auto-install timed out")
                        }
                    }
                } else {
                    logger.info("User cancelled auto-install")
                }
            }
        } catch (e: IllegalStateException) {
            logger.error("Failed to detect OS for auto-install", e)
            Notifications.Bus.notify(
                Notification(
                    NOTIFICATION_GROUP_ID,
                    "PreCI",
                    "无法识别当前操作系统，请手动下载安装。",
                    NotificationType.ERROR
                ),
                project
            )
        }
    }

    /**
     * 在 IDE 内置终端中打开安装脚本，让用户进行交互式安装
     */
    private fun openTerminalAndRunInstall(project: Project, scriptPath: String) {
        val scriptFile = File(scriptPath)
        val workingDir = scriptFile.parentFile?.absolutePath
        if (workingDir == null) {
            logger.error("Install script has no parent directory: $scriptPath")
            notifyInstallScriptError(project, scriptPath, "安装脚本路径异常，无法确定工作目录")
            return
        }
        val scriptName = scriptFile.name

        ApplicationManager.getApplication().invokeLater {
            try {
                val terminalManager = TerminalToolWindowManager.getInstance(project)
                val widget = terminalManager.createLocalShellWidget(workingDir, "PreCI Install")
                val command = when {
                    scriptName.endsWith(".ps1") ->
                        "powershell -ExecutionPolicy Bypass -File .\\$scriptName"
                    scriptName.endsWith(".bat") || scriptName.endsWith(".cmd") ->
                        scriptName
                    else ->
                        "./$scriptName"
                }
                widget.executeCommand(command)
                logger.info("Opened terminal and executed: $command in $workingDir")

                showPostInstallNotification(project)
            } catch (e: Exception) {
                logger.error("Failed to open terminal for install script", e)
                val manualCommand = if (scriptName.endsWith(".ps1")) {
                    "powershell -ExecutionPolicy Bypass -File \"$scriptPath\""
                } else {
                    scriptPath
                }
                notifyInstallScriptError(project, manualCommand, e.message)
            }
        }
    }

    private fun notifyInstallScriptError(project: Project, scriptCommand: String, detail: String?) {
        val message = buildString {
            append("无法打开终端运行安装脚本")
            if (detail != null) append("：$detail")
            append("\n请手动在终端中执行：\n$scriptCommand")
        }
        Notifications.Bus.notify(
            Notification(
                NOTIFICATION_GROUP_ID,
                "PreCI",
                message,
                NotificationType.WARNING
            ),
            project
        )
    }

    /**
     * 安装脚本已在终端启动后，提示用户手动安装的备用方案
     */
    private fun showPostInstallNotification(project: Project) {
        val installDir = File(System.getProperty("user.home"))
            .resolve(".preci").resolve("install-temp").absolutePath

        val notification = Notification(
            NOTIFICATION_GROUP_ID_INSTALL,
            "PreCI 安装",
            "安装脚本已在终端中启动，请按照提示完成安装。安装成功后请重启 ide 使用。" +
                "如果安装失败，请到以下目录手动执行安装脚本：" +
                "$installDir。" +
                "如果之前安装过旧版本 PreCI，建议先执行 uninstall_old_preci 脚本卸载旧版本。",
            NotificationType.WARNING
        )

        notification.addAction(
            NotificationAction.createSimple("重启") {
                ApplicationManagerEx.getApplicationEx().restart(true)
            }
        )

        notification.addAction(
            NotificationAction.createSimple("取消") {
                notification.expire()
            }
        )

        Notifications.Bus.notify(notification, project)
    }
}

