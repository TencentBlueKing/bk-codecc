package com.codecc.preci.ui.action

import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.checker.CheckerService
import com.codecc.preci.ui.dialog.CheckerSetDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

/**
 * 规则集选择 Action
 *
 * 打开规则集选择对话框，允许用户查看和选择代码检查规则集。
 *
 * **功能：**
 * - 显示所有可用的规则集列表
 * - 支持多选规则集
 * - 记住用户的规则集选择（可配置）
 *
 * **使用场景：**
 * - 从菜单 Tools → PreCI → Checker Sets 打开
 * - 从工具窗口工具栏打开
 *
 * **线程安全性：**
 * - Action 在 EDT 线程中执行
 * - 对话框会在后台线程加载数据，在 EDT 显示结果
 *
 * @since 1.0
 */
class CheckerSetAction : AnAction(), DumbAware {

    private val logger = PreCILogger.getLogger(CheckerSetAction::class.java)

    /**
     * 执行规则集选择操作
     *
     * 打开规则集选择对话框，用户可以在对话框中查看和选择规则集。
     *
     * @param e Action 事件
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        logger.info("打开规则集选择对话框")

        // 打开规则集选择对话框
        val dialog = CheckerSetDialog(project)
        dialog.show()
    }

    /**
     * 更新 Action 的可用状态
     *
     * @param e Action 事件
     */
    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null
    }

    /**
     * 获取 Action ID，用于快捷键绑定等
     */
    override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.BGT

    companion object {
        /**
         * Action ID，用于程序化触发
         */
        const val ACTION_ID = "PreCI.CheckerSets"
    }
}
