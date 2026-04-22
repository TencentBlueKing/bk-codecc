package com.codecc.preci.ui.action

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
 * Pre-Push 增量扫描 Action
 *
 * 触发 PreCI pre-push 增量扫描，只扫描本地已提交但未推送到远程的变更文件。
 *
 * **功能说明：**
 * - 点击菜单项时触发
 * - 调用 ScanService.prePushScan() 执行 pre-push 扫描
 * - Local Server 会自动通过 Git 获取未推送的变更文件
 * - 自动打开工具窗口并切换到进度选项卡
 * - 启动进度轮询，实时显示扫描状态
 * - 扫描结果通过 IDE 通知展示给用户
 *
 * **使用场景：**
 * - 推送代码前快速检查未推送的提交
 * - 确保推送的代码没有明显问题
 * - 节省扫描时间，只检查变更部分
 *
 * **前置条件：**
 * - 项目必须是 Git 仓库
 * - 需要有未推送到远程的提交
 *
 * @since 1.0
 */
class PrePushScanAction : AnAction() {

    /**
     * 执行 pre-push 增量扫描操作
     *
     * 在后台协程中调用 ScanService 执行 pre-push 扫描，
     * 并自动打开工具窗口显示进度。
     *
     * @param event Action 事件对象，包含项目等上下文信息
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val scanService = ScanService.getInstance(project)

        // 先打开工具窗口（确保工具窗口内容已创建）
        ApplicationManager.getApplication().invokeLater {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("PreCI")
            toolWindow?.show()
        }

        // 在后台协程中执行扫描
        CoroutineScope(Dispatchers.Default).launch {
            val result = scanService.prePushScan()

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

