package com.codecc.preci.ui.action

import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.scan.ScanResult
import com.codecc.preci.service.scan.ScanService
import com.codecc.preci.ui.toolwindow.PreCIToolWindowFactory
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 全量扫描 Action
 *
 * 触发 PreCI 全量扫描，扫描整个项目的所有代码文件。
 *
 * **功能说明：**
 * - 点击菜单项或工具栏按钮时触发
 * - 调用 ScanService.fullScan() 执行全量扫描
 * - 自动打开工具窗口
 * - 扫描启动后自动刷新结果
 *
 * **使用场景：**
 * - 用户希望对整个项目进行完整的代码检查
 * - 适合在提交前或定期进行全面的代码质量检查
 *
 * @since 1.0
 */
class FullScanAction : AnAction() {

    private val logger = PreCILogger.getLogger(FullScanAction::class.java)

    /**
     * 执行全量扫描操作
     *
     * 在后台协程中调用 ScanService 执行全量扫描，并自动打开工具窗口。
     *
     * @param event Action 事件对象，包含项目等上下文信息
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val scanService = ScanService.getInstance(project)

        logger.info("全量扫描 Action 触发")

        // 先打开工具窗口
        ApplicationManager.getApplication().invokeLater {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("PreCI")
            logger.info("打开工具窗口: toolWindow=$toolWindow")
            toolWindow?.show()
        }

        // 在后台协程中执行扫描
        CoroutineScope(Dispatchers.Default).launch {
            logger.info("开始执行全量扫描")
            val result = scanService.fullScan()
            logger.info("全量扫描结果: ${if (result is ScanResult.Success) "成功" else "失败"}")

            if (result is ScanResult.Success) {
                ApplicationManager.getApplication().invokeLater {
                    val preCIToolWindow = PreCIToolWindowFactory.getToolWindow(project)
                    preCIToolWindow?.showScanProgress()
                }
            }
        }
    }

    /**
     * 更新 Action 的可用状态
     *
     * 只有当项目存在时，Action 才可用。
     *
     * @param event Action 事件对象
     */
    override fun update(event: AnActionEvent) {
        // 只有当项目存在时，Action 才可用
        event.presentation.isEnabled = event.project != null
    }

    /**
     * 指定 Action 更新在后台线程执行
     *
     * 这是 IntelliJ Platform 推荐的做法，避免阻塞 UI 线程。
     *
     * @return ActionUpdateThread.BGT 表示在后台线程执行
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

