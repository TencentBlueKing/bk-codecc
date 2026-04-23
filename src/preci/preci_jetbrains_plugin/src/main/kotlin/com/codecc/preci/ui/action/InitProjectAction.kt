package com.codecc.preci.ui.action

import com.codecc.preci.service.scan.InitResult
import com.codecc.preci.service.scan.ScanService
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking

/**
 * 项目初始化 Action
 *
 * 在 IDE 工具栏或菜单中提供"初始化 PreCI 项目"入口，用户点击后会：
 * 1. 调用 ScanService.initProject() 初始化项目
 * 2. 显示进度提示
 * 3. 初始化完成后显示通知
 *
 * **使用场景：**
 * - 用户首次使用 PreCI 扫描功能前
 * - 项目配置丢失或损坏需要重新初始化
 * - 更换项目根目录后需要重新初始化
 *
 * **功能特性：**
 * - 后台执行：使用 IntelliJ 的 Task API 在后台执行初始化
 * - 进度提示：显示进度对话框，避免用户重复操作
 * - 友好通知：初始化完成后显示结果通知
 *
 * @since 1.0
 */
class InitProjectAction : AnAction() {

    /**
     * 执行初始化操作
     *
     * @param event Action 事件
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        // 在后台任务中执行初始化
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "初始化 PreCI 项目",
            false // 不可取消
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "正在初始化项目..."
                indicator.isIndeterminate = true

                // 调用 ScanService 初始化项目
                val scanService = ScanService.getInstance(project)
                val result = runBlocking {
                    scanService.initProject()
                }

                // 根据结果显示通知（ScanService 已经显示过通知，这里可以选择性显示）
                when (result) {
                    is InitResult.Success -> {
                        // 成功通知已由 ScanService 显示
                    }
                    is InitResult.Failure -> {
                        // 失败通知已由 ScanService 显示
                    }
                }
            }
        })
    }

    /**
     * 更新 Action 状态
     *
     * @param event Action 事件
     */
    override fun update(event: AnActionEvent) {
        // 只有在项目存在时才启用 Action
        val project = event.project
        event.presentation.isEnabled = project != null
    }
}

