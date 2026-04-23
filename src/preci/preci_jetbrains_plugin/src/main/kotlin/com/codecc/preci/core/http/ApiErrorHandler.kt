package com.codecc.preci.core.http

import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.auth.AuthService
import com.codecc.preci.service.auth.LoginResult
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking

/**
 * API 错误处理器
 *
 * 统一处理 PreCI API 调用中的特定错误，提供用户提示功能。
 *
 * **处理策略：**
 * 1. **100005 (Invalid Access Token)**：弹出登录对话框引导用户重新登录
 * 2. **100009 (Invalid Project ID)**：提示用户前往 Settings/Tools/PreCI 绑定蓝盾项目
 *
 * @since 1.0
 */
object ApiErrorHandler {
    private val logger = PreCILogger.getLogger(ApiErrorHandler::class.java)

    /**
     * 处理 API 业务异常
     *
     * 根据错误码执行相应的恢复策略或用户提示
     *
     * @param exception 业务异常
     * @param project 当前项目
     * @return true 如果错误已被处理（已提示用户），false 否则
     */
    fun handle(exception: BusinessException, project: Project?): Boolean {
        val errorCode = exception.errorCode
        logger.info("ApiErrorHandler.handle() - 错误码: $errorCode, HTTP状态码: ${exception.httpCode}, 错误信息: ${exception.message}")

        if (errorCode == null) {
            logger.debug("错误码为 null，无法处理")
            return false
        }

        return when (errorCode) {
            PreCIErrorCode.CODE_INVALID_ACCESS_TOKEN -> {
                logger.info("检测到认证错误（100005），准备弹出登录对话框")
                handleInvalidAccessToken(project)
            }
            PreCIErrorCode.CODE_INVALID_PROJECT_ID -> {
                logger.info("检测到项目 ID 无效错误（100009），准备显示通知")
                handleInvalidProjectId(project)
            }
            PreCIErrorCode.CODE_INVALID_CHECKER_SET -> {
                logger.info("检测到规则集无效错误（100010），准备显示通知")
                handleInvalidCheckerSet(project)
            }
            else -> {
                logger.debug("错误码 $errorCode 不需要特殊处理")
                false
            }
        }
    }

    /**
     * 处理 Access Token 无效错误（错误码 100005）
     *
     * 弹出登录对话框让用户重新登录。
     */
    private fun handleInvalidAccessToken(project: Project?): Boolean {
        logger.info("检测到认证失效，发起 OAuth 重新登录")

        object : Task.Backgroundable(project, "重新登录到 PreCI", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "正在执行 OAuth 登录，请在浏览器中完成认证..."

                val authService = AuthService.getInstance()
                val result = runBlocking {
                    authService.loginWithOAuth()
                }

                if (result is LoginResult.Failure) {
                    logger.warn("重新登录失败: ${result.message}")
                }
            }
        }.queue()

        return true
    }

    /**
     * 处理项目 ID 无效错误（错误码 100009）
     *
     * 提示用户前往设置页面绑定蓝盾项目
     *
     * @param exception 业务异常
     * @param project 当前项目
     * @return true（已提示用户）
     */
    private fun handleInvalidProjectId(project: Project?): Boolean {
        logger.warn("检测到蓝盾项目 ID 无效或缺失，准备显示通知")

        showNotification(
            project = project,
            title = "PreCI 项目未绑定",
            content = "蓝盾项目 ID 无效或缺失，请前往 Settings → Tools → PreCI 绑定蓝盾项目",
            type = NotificationType.WARNING,
            actionText = "打开设置",
            actionCallback = {
                if (project != null) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "PreCI")
                }
            }
        )

        logger.info("已显示项目未绑定通知")
        return true
    }

    /**
     * 处理规则集无效或缺失错误（错误码 100010）
     *
     * 提示用户前往设置页面配置扫描规则集。
     */
    private fun handleInvalidCheckerSet(project: Project?): Boolean {
        logger.warn("检测到扫描规则集无效或缺失，准备显示通知")

        showNotification(
            project = project,
            title = "扫描规则集未配置",
            content = "扫描规则集无效或缺失，请前往 Settings → Tools → PreCI 选择规则集",
            type = NotificationType.WARNING,
            actionText = "打开设置",
            actionCallback = {
                if (project != null) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "PreCI")
                }
            }
        )

        logger.info("已显示规则集未配置通知")
        return true
    }

    /**
     * 显示通知
     *
     * @param project 当前项目
     * @param title 通知标题
     * @param content 通知内容
     * @param type 通知类型
     * @param actionText 操作按钮文本（可选）
     * @param actionCallback 操作按钮回调（可选）
     */
    private fun showNotification(
        project: Project?,
        title: String,
        content: String,
        type: NotificationType,
        actionText: String? = null,
        actionCallback: (() -> Unit)? = null
    ) {
        try {
            val notificationGroup = NotificationGroupManager.getInstance()
                .getNotificationGroup("PreCI.Scan")

            if (notificationGroup == null) {
                logger.error("未找到通知组 'PreCI.Scan'")
                return
            }

            val notification = notificationGroup.createNotification(title, content, type)

            if (actionText != null && actionCallback != null) {
                notification.addAction(object : com.intellij.openapi.actionSystem.AnAction(actionText) {
                    override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                        actionCallback()
                        notification.expire()
                    }
                })
            }

            notification.notify(project)
            logger.info("通知已显示: $title")
        } catch (e: Exception) {
            logger.error("显示通知失败", e)
        }
    }
}

