package com.codecc.preci.vcs

import com.codecc.preci.core.config.PreCISettings
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.vcs.VcsCheckResult
import com.codecc.preci.service.vcs.VcsCheckService
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference

/**
 * PreCI Pre-Commit 检查处理器工厂
 *
 * 通过 IntelliJ Platform 的 `com.intellij.checkinHandlerFactory` 扩展点注册，
 * 在用户执行 Git Commit 时自动创建 [PreCICheckinHandler] 进行代码检查。
 *
 * **工作流程：**
 * 1. IDE 在打开 Commit 对话框时调用 [createHandler] 创建处理器实例
 * 2. 用户点击 Commit 按钮时，IDE 调用处理器的 [CheckinHandler.beforeCheckin]
 * 3. 处理器执行 PreCI 扫描检查并根据结果决定是否阻止提交
 *
 * @since 1.1
 */
class PreCICheckinHandlerFactory : CheckinHandlerFactory() {

    /**
     * 创建 PreCI 提交检查处理器
     *
     * @param panel 提交面板，提供当前项目等上下文信息
     * @param commitContext 提交上下文
     * @return PreCI 检查处理器实例
     */
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return PreCICheckinHandler(panel)
    }
}

/**
 * PreCI Pre-Commit 检查处理器
 *
 * 在 commit 执行前调用 PreCI Local Server 进行代码扫描，
 * 根据扫描结果决定是否阻止提交。
 *
 * **检查逻辑：**
 * - 未启用 → 直接放行（[ReturnResult.COMMIT]）
 * - 扫描无缺陷 → 放行
 * - 扫描有缺陷 → 弹出对话框，用户可选择"取消提交"或"仍然提交"
 * - 扫描出错 → 放行（不因工具故障阻塞用户）
 *
 * @property panel 提交面板
 * @since 1.1
 */
class PreCICheckinHandler(
    private val panel: CheckinProjectPanel
) : CheckinHandler() {

    private val logger = PreCILogger.getLogger(PreCICheckinHandler::class.java)

    @Suppress("TooGenericExceptionCaught")
    override fun beforeCheckin(): ReturnResult {
        val settings = PreCISettings.getInstance()
        if (!settings.preCommitCheckEnabled) {
            return ReturnResult.COMMIT
        }

        val project = panel.project
        logger.info("Pre-Commit 检查开始")

        val resultRef = AtomicReference<VcsCheckResult>(VcsCheckResult.Disabled)

        ProgressManager.getInstance().run(object : Task.Modal(project, "PreCI Pre-Commit 代码检查", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "正在执行 PreCI 代码扫描..."

                try {
                    val vcsCheckService = VcsCheckService.getInstance(project)
                    val result = runBlocking {
                        vcsCheckService.performPreCommitCheck()
                    }
                    resultRef.set(result)
                } catch (e: Exception) {
                    logger.error("Pre-Commit 检查执行异常: ${e.message}", e)
                    resultRef.set(VcsCheckResult.Error("检查执行异常: ${e.message}", e))
                }
            }
        })

        return handleResult(resultRef.get(), "Pre-Commit")
    }

    /**
     * 根据检查结果决定返回值
     *
     * @param result 检查结果
     * @param label 日志标签
     * @return commit 操作的返回结果
     */
    private fun handleResult(result: VcsCheckResult, label: String): ReturnResult {
        return when (result) {
            is VcsCheckResult.Disabled -> {
                ReturnResult.COMMIT
            }
            is VcsCheckResult.NoDefects -> {
                logger.info("$label 检查通过，允许提交")
                ReturnResult.COMMIT
            }
            is VcsCheckResult.HasDefects -> {
                logger.warn("$label 检查发现 ${result.defectCount} 个缺陷")
                val choice = Messages.showYesNoDialog(
                    panel.project,
                    "PreCI 代码检查发现 ${result.defectCount} 个问题。\n\n" +
                        "是否仍然提交？\n" +
                        "（选择「取消提交」，您可以在 PreCI 工具窗口查看详细结果）",
                    "PreCI Pre-Commit 检查",
                    "仍然提交",
                    "取消提交",
                    Messages.getWarningIcon()
                )
                if (choice == Messages.YES) {
                    logger.info("用户选择仍然提交（Commit Anyway）")
                    ReturnResult.COMMIT
                } else {
                    logger.info("用户取消提交")
                    ReturnResult.CANCEL
                }
            }
            is VcsCheckResult.Error -> {
                logger.warn("$label 检查出错，放行: ${result.message}")
                ReturnResult.COMMIT
            }
        }
    }
}
