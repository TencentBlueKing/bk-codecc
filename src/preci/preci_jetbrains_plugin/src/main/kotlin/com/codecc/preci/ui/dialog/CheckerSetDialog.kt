package com.codecc.preci.ui.dialog

import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.checker.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * 规则集选择对话框
 *
 * 提供一个可视化界面，让用户查看和选择代码检查规则集。
 * 支持多选，选择的规则集会应用到后续的代码扫描中。
 *
 * **功能：**
 * - 显示所有可用规则集（包括 ID、名称、对应工具）
 * - 支持多选（使用复选框）
 * - 显示已保存的规则集选择
 * - 提供"记住选择"选项
 * - 实时显示加载状态
 *
 * **界面布局：**
 * ```
 * +------------------------------------------+
 * |  选择要使用的规则集：                     |
 * +------------------------------------------+
 * |  [x] Go 标准规则集 (golangci-lint)       |
 * |  [x] 安全基础规则集 (gosec)              |
 * |  [ ] Python 规则集 (pylint)              |
 * |  ...                                     |
 * +------------------------------------------+
 * |  [x] 记住我的选择                        |
 * +------------------------------------------+
 * |              [取消]  [确定]              |
 * +------------------------------------------+
 * ```
 *
 * **使用示例：**
 * ```kotlin
 * val dialog = CheckerSetDialog(project)
 * if (dialog.showAndGet()) {
 *     // 用户点击了确定，规则集已选择
 *     val selectedSets = dialog.getSelectedCheckerSets()
 * }
 * ```
 *
 * @property project 当前项目
 *
 * @since 1.0
 */
class CheckerSetDialog(private val project: Project) : DialogWrapper(project) {

    private val logger = PreCILogger.getLogger(CheckerSetDialog::class.java)

    /**
     * 规则集服务
     */
    private val checkerService: CheckerService = CheckerService.getInstance(project)

    /**
     * 规则集复选框列表
     */
    private lateinit var checkerSetList: CheckBoxList<CheckerSetItem>

    /**
     * 记住选择复选框
     */
    private lateinit var rememberCheckBox: JCheckBox

    /**
     * 加载状态标签
     */
    private lateinit var statusLabel: JBLabel

    /**
     * 主面板
     */
    private lateinit var mainPanel: JPanel

    /**
     * 加载的规则集列表
     */
    private var loadedCheckerSets: List<CheckerSetInfo> = emptyList()

    /**
     * 是否正在加载
     */
    @Volatile
    private var isLoading = false

    init {
        title = "选择规则集"
        setOKButtonText("确定")
        setCancelButtonText("取消")
        init()

        // 异步加载规则集列表
        loadCheckerSets()
    }

    override fun createCenterPanel(): JComponent {
        mainPanel = JPanel(BorderLayout())
        mainPanel.preferredSize = Dimension(500, 400)
        mainPanel.border = JBUI.Borders.empty(10)

        // 标题标签
        val titleLabel = JBLabel("选择要使用的规则集：")
        titleLabel.border = JBUI.Borders.emptyBottom(10)
        mainPanel.add(titleLabel, BorderLayout.NORTH)

        // 规则集列表（带复选框）
        checkerSetList = CheckBoxList()
        checkerSetList.emptyText.text = "正在加载规则集列表..."

        val scrollPane = JBScrollPane(checkerSetList)
        scrollPane.preferredSize = Dimension(480, 300)
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        // 底部面板：记住选择 + 状态标签
        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.border = JBUI.Borders.emptyTop(10)

        rememberCheckBox = JCheckBox("记住我的选择", checkerService.isRememberCheckerSetsEnabled())
        rememberCheckBox.toolTipText = "启用后，下次打开项目时会自动应用保存的规则集选择"
        bottomPanel.add(rememberCheckBox, BorderLayout.WEST)

        statusLabel = JBLabel("")
        statusLabel.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        bottomPanel.add(statusLabel, BorderLayout.EAST)

        mainPanel.add(bottomPanel, BorderLayout.SOUTH)

        return mainPanel
    }

    /**
     * 异步加载规则集列表
     *
     * 使用 IntelliJ Platform 的后台任务 API 进行异步加载，
     * 避免与 IDE 的协程系统冲突。
     */
    private fun loadCheckerSets() {
        logger.info("开始异步加载规则集列表")
        isLoading = true
        updateLoadingStatus("正在加载...")
        okAction.isEnabled = false

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "加载规则集列表", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "正在获取规则集列表..."

                logger.info("后台任务开始执行，获取规则集列表")

                try {
                    // 使用同步阻塞版本的方法，避免协程冲突
                    val result = checkerService.getCheckerSetListBlocking()

                    logger.info("规则集列表获取完成，结果类型: ${result::class.simpleName}")

                    // 在 EDT 更新 UI，使用 ModalityState.any() 确保在模态对话框中也能立即执行
                    ApplicationManager.getApplication().invokeLater({
                        logger.info("开始在 EDT 中更新 UI")
                        try {
                            when (result) {
                                is CheckerSetListResult.Success -> {
                                    logger.info("处理成功结果，规则集数量: ${result.checkerSets.size}")
                                    loadedCheckerSets = result.checkerSets
                                    updateCheckerSetList(result.checkerSets)
                                    updateLoadingStatus("已加载 ${result.checkerSets.size} 个规则集")
                                    okAction.isEnabled = true
                                    logger.info("UI 更新完成")
                                }
                                is CheckerSetListResult.Failure -> {
                                    logger.error("处理失败结果: ${result.message}")
                                    checkerSetList.emptyText.text = "加载失败：${result.message}"
                                    updateLoadingStatus("加载失败")
                                    logger.error("加载规则集列表失败: ${result.message}")
                                }
                            }
                        } catch (uiException: Exception) {
                            logger.error("UI 更新过程中发生异常", uiException)
                            checkerSetList.emptyText.text = "UI 更新异常：${uiException.message}"
                            updateLoadingStatus("UI 更新异常")
                        } finally {
                            isLoading = false
                            logger.info("加载状态重置完成")
                        }
                    }, ModalityState.any())
                } catch (e: Exception) {
                    logger.error("后台任务执行异常", e)
                    ApplicationManager.getApplication().invokeLater({
                        try {
                            checkerSetList.emptyText.text = "加载异常：${e.message}"
                            updateLoadingStatus("加载异常")
                            logger.error("加载规则集列表异常", e)
                        } finally {
                            isLoading = false
                        }
                    }, ModalityState.any())
                }
            }
        })
    }

    /**
     * 更新规则集列表 UI
     *
     * @param checkerSets 规则集列表
     */
    private fun updateCheckerSetList(checkerSets: List<CheckerSetInfo>) {
        logger.info("开始更新规则集列表 UI，规则集数量: ${checkerSets.size}")

        // 获取已保存的选择
        val savedSets = checkerService.loadSelectedCheckerSets().toSet()
        logger.info("已保存的规则集选择: $savedSets")

        // 清空并重新填充列表
        checkerSetList.clear()
        logger.info("已清空规则集列表")

        checkerSets.forEachIndexed { index, set ->
            val item = CheckerSetItem(set)
            val isSelected = savedSets.contains(set.id)
            checkerSetList.addItem(item, item.displayText, isSelected)
            logger.debug("添加规则集项 $index: ${set.id} - ${set.name} (${set.toolName}), 选中: $isSelected")
        }

        // 刷新 UI
        checkerSetList.revalidate()
        checkerSetList.repaint()
        logger.info("UI 刷新完成")

        logger.info("规则集列表已更新，共 ${checkerSets.size} 个，已选择 ${savedSets.size} 个")
    }

    /**
     * 更新加载状态标签
     *
     * @param text 状态文本
     */
    private fun updateLoadingStatus(text: String) {
        statusLabel.text = text
    }

    /**
     * 获取用户选择的规则集 ID 列表
     *
     * @return 选择的规则集 ID 列表
     */
    fun getSelectedCheckerSets(): List<String> {
        val selectedIds = mutableListOf<String>()

        for (i in 0 until checkerSetList.model.size) {
            if (checkerSetList.isItemSelected(i)) {
                val item = checkerSetList.getItemAt(i)
                if (item != null) {
                    selectedIds.add(item.checkerSetInfo.id)
                }
            }
        }

        return selectedIds
    }

    override fun doOKAction() {
        if (isLoading) {
            return
        }

        val selectedSets = getSelectedCheckerSets()

        logger.info("用户选择了 ${selectedSets.size} 个规则集: $selectedSets")

        // 更新"记住选择"配置
        checkerService.setRememberCheckerSetsEnabled(rememberCheckBox.isSelected)

        // 使用后台任务选择规则集
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "选择规则集", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "正在应用规则集选择..."

                try {
                    // 使用同步阻塞版本的方法，避免协程冲突
                    val result = checkerService.selectCheckerSetsBlocking(selectedSets)

                    // 在 EDT 显示结果
                    ApplicationManager.getApplication().invokeLater {
                        when (result) {
                            is CheckerSetSelectResult.Success -> {
                                logger.info("规则集选择成功: ${result.selectedSets}")
                                // 关闭对话框
                                close(OK_EXIT_CODE)
                            }
                            is CheckerSetSelectResult.Failure -> {
                                logger.error("规则集选择失败: ${result.message}")
                                Messages.showErrorDialog(
                                    project,
                                    "选择规则集失败：${result.message}",
                                    "错误"
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        logger.error("规则集选择异常", e)
                        Messages.showErrorDialog(
                            project,
                            "选择规则集异常：${e.message}",
                            "错误"
                        )
                    }
                }
            }
        })
    }

    override fun doCancelAction() {
        logger.info("用户取消了规则集选择")
        super.doCancelAction()
    }

    /**
     * 规则集列表项
     *
     * 包装 [CheckerSetInfo]，提供显示文本
     */
    private data class CheckerSetItem(
        val checkerSetInfo: CheckerSetInfo
    ) {
        /**
         * 显示文本：规则集名称 (工具名称)
         */
        val displayText: String
            get() = "${checkerSetInfo.name} (${checkerSetInfo.toolName})"

        override fun toString(): String = displayText
    }
}
