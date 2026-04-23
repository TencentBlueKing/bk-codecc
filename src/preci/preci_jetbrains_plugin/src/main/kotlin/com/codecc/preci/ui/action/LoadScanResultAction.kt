package com.codecc.preci.ui.action

import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.scan.ScanResultQueryResult
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
 * 加载扫描结果 Action
 *
 * 从 PreCI Local Server 加载历史扫描结果，无需执行新的扫描。
 * 适用于查看之前的扫描结果，或当扫描已完成但结果未自动加载时手动刷新。
 *
 * **功能说明：**
 * - 点击菜单项或工具栏按钮时触发
 * - 调用 ScanService.getScanResult() 获取历史扫描结果
 * - 自动打开工具窗口并切换到"扫描结果"选项卡
 * - 显示扫描结果列表（代码缺陷）
 * - 如果无结果，显示"未发现代码问题"提示
 *
 * **使用场景：**
 * - 查看历史扫描结果
 * - 扫描完成后结果未自动加载时手动刷新
 * - 在其他项目扫描后查看当前项目的结果
 * - 验证扫描结果是否已更新
 *
 * **注意事项：**
 * - 需要 PreCI Local Server 正在运行
 * - 需要项目已初始化
 * - 如果从未执行过扫描，可能返回空结果
 * - 结果来自 Local Server 的缓存，可能不是最新的
 *
 * @since 1.0
 */
class LoadScanResultAction : AnAction() {

    private val logger = PreCILogger.getLogger(LoadScanResultAction::class.java)

    /**
     * 执行加载扫描结果操作
     *
     * 在后台协程中调用 ScanService 获取扫描结果，
     * 并自动打开工具窗口显示结果。
     *
     * @param event Action 事件对象，包含项目等上下文信息
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        logger.info("加载扫描结果 Action 触发")

        // 先打开工具窗口
        ApplicationManager.getApplication().invokeLater {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("PreCI")
            logger.info("打开工具窗口: toolWindow=$toolWindow")
            toolWindow?.show()
            
            // 刷新结果
            val preCIToolWindow = PreCIToolWindowFactory.getToolWindow(project)
            preCIToolWindow?.refresh()
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

