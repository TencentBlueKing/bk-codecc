package com.codecc.preci.vcs

import com.codecc.preci.core.config.PreCISettings
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.vcs.VcsCheckResult
import com.codecc.preci.service.vcs.VcsCheckService
import com.intellij.dvcs.push.PrePushHandler
import com.intellij.dvcs.push.PushInfo
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.runBlocking
import javax.swing.SwingUtilities

/**
 * PreCI Pre-Push 检查处理器
 *
 * 通过 IntelliJ Platform 的 `com.intellij.prePushHandler` 扩展点注册，
 * 在用户执行 Git Push 时自动执行代码检查。
 *
 * **工作流程：**
 * 1. 用户在 Push 对话框点击 Push 按钮
 * 2. IDE 调用 [handle] 方法执行检查
 * 3. 检查完成后根据结果返回 OK（放行）或 ABORT（阻止）
 *
 * **容错策略：**
 * 与 pre-commit 一致，检查出错时默认放行。
 *
 * @since 1.1
 */
class PreCIPrePushHandler : PrePushHandler {

    private val logger = PreCILogger.getLogger(PreCIPrePushHandler::class.java)

    override fun getPresentableName(): String = "PreCI Code Check"

    @Suppress("TooGenericExceptionCaught")
    override fun handle(pushDetails: MutableList<PushInfo>, indicator: ProgressIndicator): PrePushHandler.Result {
        val settings = PreCISettings.getInstance()
        if (!settings.prePushCheckEnabled) {
            return PrePushHandler.Result.OK
        }

        logger.info("Pre-Push 检查开始")
        indicator.text = "正在执行 PreCI Pre-Push 代码检查..."
        indicator.isIndeterminate = true

        // 从 PushInfo 中获取 project
        val project = pushDetails.firstOrNull()?.repository?.project
        if (project == null) {
            logger.warn("无法获取项目信息，放行 Push")
            return PrePushHandler.Result.OK
        }

        return try {
            val vcsCheckService = VcsCheckService.getInstance(project)
            val result = runBlocking {
                vcsCheckService.performPrePushCheck()
            }

            when (result) {
                is VcsCheckResult.Disabled -> PrePushHandler.Result.OK
                is VcsCheckResult.NoDefects -> {
                    logger.info("Pre-Push 检查通过")
                    PrePushHandler.Result.OK
                }
                is VcsCheckResult.HasDefects -> {
                    logger.warn("Pre-Push 检查发现 ${result.defectCount} 个缺陷")
                    showPushAnywayDialog(result, project)
                }
                is VcsCheckResult.Error -> {
                    logger.warn("Pre-Push 检查出错，放行: ${result.message}")
                    PrePushHandler.Result.OK
                }
            }
        } catch (e: Exception) {
            logger.error("Pre-Push 检查执行异常: ${e.message}", e)
            PrePushHandler.Result.OK
        }
    }

    /**
     * 在 EDT 上显示 Push Anyway 确认对话框
     *
     * @param result 包含缺陷信息的检查结果
     * @param project 当前项目
     * @return ABORT（取消推送）或 OK（继续推送）
     */
    private fun showPushAnywayDialog(
        result: VcsCheckResult.HasDefects,
        project: com.intellij.openapi.project.Project
    ): PrePushHandler.Result {
        var userChoice = Messages.NO
        SwingUtilities.invokeAndWait {
            userChoice = Messages.showYesNoDialog(
                project,
                "PreCI 代码检查发现 ${result.defectCount} 个问题。\n\n" +
                    "是否仍然推送？\n" +
                    "（选择「否」取消推送，您可以在 PreCI 工具窗口查看详细结果）",
                "PreCI Pre-Push 检查",
                "仍然推送",
                "取消推送",
                Messages.getWarningIcon()
            )
        }
        return if (userChoice == Messages.YES) {
            logger.info("用户选择仍然推送（Push Anyway）")
            PrePushHandler.Result.OK
        } else {
            logger.info("用户取消推送")
            PrePushHandler.Result.ABORT
        }
    }
}
