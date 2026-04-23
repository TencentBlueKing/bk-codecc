package com.codecc.preci.ui.action

import com.codecc.preci.service.scan.ScanResult
import com.codecc.preci.service.scan.ScanService
import com.codecc.preci.ui.toolwindow.PreCIToolWindowFactory
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 目标扫描 Action
 *
 * 触发 PreCI 目标扫描，只扫描用户选定的文件或目录。
 *
 * **功能说明：**
 * - 在项目视图或编辑器右键菜单中触发
 * - 获取用户选中的文件或目录
 * - 调用 ScanService.targetScan() 执行目标扫描
 * - 自动打开工具窗口并切换到进度选项卡
 * - 启动进度轮询，实时显示扫描状态
 * - 扫描结果通过 IDE 通知展示给用户
 *
 * **使用场景：**
 * - 用户只想检查特定的文件或目录
 * - 适合快速检查刚修改的代码
 * - 节省扫描时间，提高效率
 *
 * @since 1.0
 */
class TargetScanAction : AnAction() {

    /**
     * 执行目标扫描操作
     *
     * 获取用户选中的文件或目录，在后台协程中调用 ScanService 执行目标扫描，
     * 并自动打开工具窗口显示进度。
     *
     * @param event Action 事件对象，包含项目和选中文件等上下文信息
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val selectedFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return

        // 转换为绝对路径列表
        val paths = selectedFiles.map { it.path }.filter { it.isNotEmpty() }

        if (paths.isEmpty()) {
            return
        }

        val scanService = ScanService.getInstance(project)

        // 先打开工具窗口（确保工具窗口内容已创建）
        ApplicationManager.getApplication().invokeLater {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("PreCI")
            toolWindow?.show()
        }

        // 在后台协程中执行扫描
        CoroutineScope(Dispatchers.Default).launch {
            val result = scanService.targetScan(paths)

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
     * 只有当项目存在且有选中的文件时，Action 才可用。
     *
     * @param event Action 事件对象
     */
    override fun update(event: AnActionEvent) {
        val project = event.project
        val selectedFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        // 只有当项目存在且有选中的文件时，Action 才可用
        event.presentation.isEnabled = project != null &&
                                      selectedFiles != null &&
                                      selectedFiles.isNotEmpty()
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

