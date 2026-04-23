package com.codecc.preci.ui.toolwindow

import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.scan.CancelScanResult
import com.codecc.preci.service.scan.ScanProgressResult
import com.codecc.preci.service.scan.ScanService
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingUtilities

/**
 * 扫描进度面板
 *
 * 提供扫描任务的进度监控和控制功能，包括：
 * - 实时显示扫描状态（运行中/已完成/空闲）
 * - 显示各工具的扫描进度
 * - 提供取消扫描按钮
 * - 定时轮询扫描进度
 *
 * **功能特性：**
 * - 自动轮询：启动后自动每隔 1 秒查询一次扫描进度
 * - 实时更新：动态更新工具状态和整体进度
 * - 取消功能：支持用户取消正在进行的扫描任务
 * - 状态指示：使用进度条和文本清晰展示当前状态
 *
 * **布局结构：**
 * ```
 * ┌────────────────────────────────────────┐
 * │  整体状态: [运行中 / 已完成 / 空闲]      │
 * ├────────────────────────────────────────┤
 * │  工具 1: [running/done]   █████░░      │
 * │  工具 2: [running/done]   ████████     │
 * │  ...                                   │
 * ├────────────────────────────────────────┤
 * │  [ 取消扫描 ]                           │
 * └────────────────────────────────────────┘
 * ```
 *
 * @property project 当前项目
 * @property coroutineScope 协程作用域，用于异步操作
 * @since 1.0
 */
class ScanProgressPanel(
    private val project: Project,
    private val coroutineScope: CoroutineScope
) {

    private val logger = PreCILogger.getLogger(ScanProgressPanel::class.java)

    /**
     * 主面板
     */
    private val mainPanel: JPanel = JBPanel<JBPanel<*>>(BorderLayout())

    /**
     * 状态标签
     */
    private val statusLabel: JBLabel = JBLabel("状态: 空闲").apply {
        font = font.deriveFont(12f)
    }

    /**
     * 项目根目录标签
     */
    private val projectRootLabel: JBLabel = JBLabel("").apply {
        foreground = JBColor.GRAY
        font = font.deriveFont(10f)
    }

    /**
     * 工具状态面板（动态内容）
     */
    private val toolStatusPanel: JPanel = JBPanel<JBPanel<*>>().apply {
        layout = GridBagLayout()
        border = JBUI.Borders.empty(4, 6)
    }

    /**
     * 取消按钮
     */
    private val cancelButton: JButton = JButton("取消扫描").apply {
        isEnabled = false
        addActionListener {
            handleCancelScan()
        }
    }

    /**
     * 整体进度条
     */
    private val overallProgressBar: JProgressBar = JProgressBar().apply {
        isIndeterminate = false
        isVisible = false
        isStringPainted = true
        preferredSize = java.awt.Dimension(preferredSize.width, JBUI.scale(16))
        maximumSize = java.awt.Dimension(Int.MAX_VALUE, JBUI.scale(16))
    }

    /**
     * 轮询任务
     */
    private var pollingJob: Job? = null

    /**
     * 轮询间隔（毫秒）
     */
    private val pollingIntervalMs = 1000L

    /**
     * 扫描完成回调
     */
    private var onScanCompleteCallback: (() -> Unit)? = null

    /**
     * 扫描取消回调（用于通知父面板切换回结果视图）
     */
    private var onScanCancelCallback: (() -> Unit)? = null

    init {
        initializeLayout()
    }

    /**
     * 初始化面板布局
     */
    private fun initializeLayout() {
        val contentPanel = JBPanel<JBPanel<*>>(BorderLayout())
        contentPanel.border = JBUI.Borders.empty(4, 6)

        val headerPanel = JBPanel<JBPanel<*>>()
        headerPanel.layout = GridBagLayout()
        headerPanel.border = BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
            JBUI.Borders.empty(3, 0)
        )

        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(1, 0)
        headerPanel.add(statusLabel, gbc)

        gbc.gridy = 1
        headerPanel.add(projectRootLabel, gbc)

        gbc.gridy = 2
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.insets = JBUI.insets(3, 0, 1, 0)
        headerPanel.add(overallProgressBar, gbc)

        contentPanel.add(headerPanel, BorderLayout.NORTH)

        contentPanel.add(toolStatusPanel, BorderLayout.CENTER)

        val buttonPanel = JBPanel<JBPanel<*>>()
        buttonPanel.border = JBUI.Borders.empty(2, 0)
        buttonPanel.add(cancelButton)
        contentPanel.add(buttonPanel, BorderLayout.SOUTH)

        mainPanel.add(contentPanel, BorderLayout.CENTER)
    }

    /**
     * 开始轮询扫描进度
     *
     * 每隔固定时间（默认 1 秒）查询一次扫描进度，并更新 UI
     */
    fun startPolling() {
        // 停止之前的轮询任务
        stopPolling()

        logger.info("开始轮询扫描进度")

        pollingJob = coroutineScope.launch {
            while (isActive) {
                try {
                    updateProgress()
                    delay(pollingIntervalMs)
                } catch (e: Exception) {
                    logger.error("轮询扫描进度失败: ${e.message}", e)
                    delay(pollingIntervalMs * 2) // 失败时延长等待时间
                }
            }
        }
    }

    /**
     * 停止轮询扫描进度
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        logger.info("停止轮询扫描进度")
    }

    /**
     * 更新扫描进度
     *
     * 调用 ScanService 查询最新进度并更新 UI
     */
    @Suppress("TooGenericExceptionCaught") // 需要捕获所有异常以保证健壮性
    private suspend fun updateProgress() {
        try {
            val scanService = ScanService.getInstance(project)
            val result = scanService.getScanProgress()

            // 在 Swing EDT 线程中更新 UI
            SwingUtilities.invokeLater {
                when (result) {
                    is ScanProgressResult.Success -> {
                        showProgress(
                            result.response.status,
                            result.response.projectRoot,
                            result.response.toolStatuses
                        )

                        // 如果扫描已完成，停止轮询并触发回调
                        if (result.response.status == "done") {
                            stopPolling()
                            onScanCompleteCallback?.invoke()
                        }
                    }
                    is ScanProgressResult.Failure -> {
                        logger.warn("查询扫描进度失败: ${result.message}")
                        // 失败时不更新 UI，保持上次状态
                    }
                }
            }

        } catch (e: Exception) {
            logger.error("更新扫描进度异常: ${e.message}", e)
        }
    }

    /**
     * 显示扫描进度
     *
     * @param status 整体状态（running/done/空字符串）
     * @param projectRoot 项目根目录
     * @param toolStatuses 各工具状态映射
     */
    private fun showProgress(
        status: String,
        projectRoot: String,
        toolStatuses: Map<String, String>
    ) {
        // 更新状态标签
        when (status) {
            "running" -> {
                statusLabel.text = "状态: 扫描进行中"
                statusLabel.foreground = JBColor.ORANGE
                cancelButton.isEnabled = true
                overallProgressBar.isVisible = true
                overallProgressBar.isIndeterminate = true
            }
            "done" -> {
                statusLabel.text = "状态: 扫描已完成"
                statusLabel.foreground = JBColor.GREEN
                cancelButton.isEnabled = false
                overallProgressBar.isVisible = false
            }
            else -> {
                statusLabel.text = "状态: 空闲"
                statusLabel.foreground = JBColor.GRAY
                cancelButton.isEnabled = false
                overallProgressBar.isVisible = false
            }
        }

        // 更新项目根目录标签
        projectRootLabel.text = if (projectRoot.isNotEmpty()) "项目: $projectRoot" else ""

        // 更新工具状态面板
        updateToolStatusPanel(toolStatuses)
    }

    /**
     * 更新工具状态面板
     *
     * @param toolStatuses 工具状态映射
     */
    private fun updateToolStatusPanel(toolStatuses: Map<String, String>) {
        // 清空面板
        toolStatusPanel.removeAll()

        if (toolStatuses.isEmpty()) {
            val emptyLabel = JBLabel("无扫描任务").apply {
                foreground = JBColor.GRAY
            }
            toolStatusPanel.add(emptyLabel)
        } else {
            val gbc = GridBagConstraints()
            gbc.gridx = 0
            gbc.gridy = GridBagConstraints.RELATIVE
            gbc.anchor = GridBagConstraints.WEST
            gbc.fill = GridBagConstraints.HORIZONTAL
            gbc.weightx = 1.0
            gbc.insets = JBUI.insets(1, 0)

            toolStatuses.forEach { (toolName, toolStatus) ->
                val toolLabel = JBLabel("$toolName: ").apply {
                    font = font.deriveFont(11f)
                }

                val statusLabel = JBLabel(
                    when (toolStatus) {
                        "running" -> "运行中 ●"
                        "done" -> "已完成 ✓"
                        else -> toolStatus
                    }
                ).apply {
                    font = font.deriveFont(11f)
                    foreground = when (toolStatus) {
                        "running" -> JBColor.ORANGE
                        "done" -> JBColor.GREEN
                        else -> JBColor.GRAY
                    }
                }

                val rowPanel = JBPanel<JBPanel<*>>()
                rowPanel.add(toolLabel)
                rowPanel.add(statusLabel)

                toolStatusPanel.add(rowPanel, gbc)
            }
        }

        // 刷新面板
        toolStatusPanel.revalidate()
        toolStatusPanel.repaint()
    }

    /**
     * 处理取消扫描按钮点击
     */
    @Suppress("TooGenericExceptionCaught") // 需要捕获所有异常以保证健壮性
    private fun handleCancelScan() {
        coroutineScope.launch {
            try {
                logger.info("用户请求取消扫描")

                val scanService = ScanService.getInstance(project)
                val result = scanService.cancelScan()

                when (result) {
                    is CancelScanResult.Success -> {
                        stopPolling()
                        SwingUtilities.invokeLater {
                            showProgress("", "", emptyMap())
                            onScanCancelCallback?.invoke()
                            showNotification(
                                "扫描已取消",
                                "已成功取消扫描任务",
                                NotificationType.INFORMATION
                            )
                        }
                    }
                    is CancelScanResult.Failure -> {
                        SwingUtilities.invokeLater {
                            showNotification(
                                "取消失败",
                                result.message,
                                NotificationType.ERROR
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                logger.error("取消扫描异常: ${e.message}", e)
                SwingUtilities.invokeLater {
                    showNotification(
                        "取消失败",
                        "取消扫描时发生错误: ${e.message}",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    /**
     * 显示 IDE 通知
     *
     * @param title 通知标题
     * @param content 通知内容
     * @param type 通知类型
     */
    @Suppress("TooGenericExceptionCaught") // 通知失败不应影响主流程
    private fun showNotification(title: String, content: String, type: NotificationType) {
        try {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("PreCI.Scan")
                .createNotification(title, content, type)
                .notify(project)
        } catch (e: Exception) {
            logger.error("显示通知失败: ${e.message}", e)
        }
    }

    /**
     * 设置扫描完成回调
     *
     * @param callback 扫描完成时的回调函数
     */
    fun setOnScanCompleteCallback(callback: () -> Unit) {
        onScanCompleteCallback = callback
    }

    /**
     * 设置扫描取消回调
     *
     * 当用户通过进度面板的"取消扫描"按钮取消扫描时触发，
     * 用于通知父面板切换回结果视图并恢复按钮状态。
     *
     * @param callback 扫描取消时的回调函数
     */
    fun setOnScanCancelCallback(callback: () -> Unit) {
        onScanCancelCallback = callback
    }

    /**
     * 清除状态
     *
     * 重置为初始空闲状态
     */
    fun clear() {
        stopPolling()
        SwingUtilities.invokeLater {
            showProgress("", "", emptyMap())
        }
    }

    /**
     * 获取主面板组件
     *
     * @return 主面板
     */
    fun getContent(): JComponent {
        return mainPanel
    }
}

