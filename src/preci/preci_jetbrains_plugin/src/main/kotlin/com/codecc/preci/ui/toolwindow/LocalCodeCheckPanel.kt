package com.codecc.preci.ui.toolwindow

import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.scan.ScanResult
import com.codecc.preci.service.scan.ScanService
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

/**
 * 本地代码检查面板
 *
 * 提供本地代码扫描功能，通过扫描范围按钮选择扫描模式：
 * - 当前文件：扫描当前打开的文件
 * - 变更文件：扫描 Git 暂存区的变更文件
 * - 所有文件：扫描项目所有文件
 *
 * 三种扫描范围共用同一个告警展示面板，因为 PreCI 底层服务只保存最后一次扫描的告警。
 * 扫描范围按钮仅影响"开始扫描"时的传入参数，不切换展示页面。
 *
 * **布局结构：**
 * ```
 * ┌────────────────────────────────────────┐
 * │ ▶ │ [当前文件] [变更文件] [所有文件]    │
 * │ ⏹ │────────────────────────────────────│
 * │ ↻ │                                    │
 * │ 🗑 │  扫描结果展示区域（共用）           │
 * │   │  (GroupedScanResultPanel)          │
 * │   │                                    │
 * └────────────────────────────────────────┘
 * ```
 *
 * @property project 当前项目
 * @property coroutineScope 协程作用域
 * @since 1.0
 */
class LocalCodeCheckPanel(
    private val project: Project,
    private val coroutineScope: CoroutineScope
) {

    private val logger = PreCILogger.getLogger(LocalCodeCheckPanel::class.java)

    /**
     * 主面板
     */
    private val mainPanel: JPanel = JBPanel<JBPanel<*>>(BorderLayout())

    // ========== 扫描范围选择按钮 ==========

    /**
     * 扫描范围索引常量
     */
    companion object {
        /** 当前文件扫描 */
        const val SCOPE_CURRENT_FILE = 0
        /** 变更文件扫描 */
        const val SCOPE_CHANGED_FILES = 1
        /** 所有文件扫描 */
        const val SCOPE_ALL_FILES = 2

        private const val CARD_RESULTS = "results"
        private const val CARD_PROGRESS = "progress"
    }

    /**
     * 当前文件按钮
     */
    private val currentFileButton: JToggleButton = JToggleButton("当前文件").apply {
        isSelected = true
    }

    /**
     * 变更文件按钮
     */
    private val changedFilesButton: JToggleButton = JToggleButton("变更文件")

    /**
     * 所有文件按钮
     */
    private val allFilesButton: JToggleButton = JToggleButton("所有文件")

    /**
     * 扫描范围按钮组（互斥选择）
     */
    private val scopeButtonGroup: ButtonGroup = ButtonGroup().apply {
        add(currentFileButton)
        add(changedFilesButton)
        add(allFilesButton)
    }

    /**
     * 共用的扫描结果面板（折叠式分组显示）
     *
     * PreCI 底层服务只保存最后一次扫描的告警，因此三种扫描范围共用同一个结果面板。
     */
    private val scanResultPanel: GroupedScanResultPanel = GroupedScanResultPanel(project, coroutineScope)

    /**
     * 扫描进度面板
     */
    private val scanProgressPanel: ScanProgressPanel = ScanProgressPanel(project, coroutineScope)

    /**
     * 内容区域（用于在结果视图和进度视图之间切换）
     */
    private val contentPanel: JPanel = JBPanel<JBPanel<*>>().apply {
        layout = java.awt.CardLayout()
    }

    /**
     * CardLayout 实例，用于切换视图
     */
    private val cardLayout: java.awt.CardLayout = contentPanel.layout as java.awt.CardLayout

    /**
     * 播放按钮（启动扫描）
     */
    private val playButton: JButton = JButton(AllIcons.Actions.Execute).apply {
        toolTipText = "开始扫描"
        isBorderPainted = false
        isContentAreaFilled = false
        addActionListener { handleStartScan() }
    }

    /**
     * 停止按钮
     */
    private val stopButton: JButton = JButton(AllIcons.Actions.Suspend).apply {
        toolTipText = "停止扫描"
        isBorderPainted = false
        isContentAreaFilled = false
        isEnabled = false
        addActionListener { handleStopScan() }
    }

    /**
     * 刷新按钮
     */
    private val refreshButton: JButton = JButton(AllIcons.Actions.Refresh).apply {
        toolTipText = "刷新结果"
        isBorderPainted = false
        isContentAreaFilled = false
        addActionListener { handleRefreshResults() }
    }

    /**
     * 清除按钮
     */
    private val clearButton: JButton = JButton(AllIcons.Actions.GC).apply {
        toolTipText = "清除结果"
        isBorderPainted = false
        isContentAreaFilled = false
        addActionListener { handleClearResults() }
    }

    /**
     * 严重告警筛选按钮（红色）
     */
    private val criticalFilterButton: JToggleButton = JToggleButton(GroupedScanResultPanel.Icons.ERROR).apply {
        toolTipText = "显示/隐藏严重告警"
        isBorderPainted = false
        isContentAreaFilled = false
        isSelected = true
        addActionListener { handleFilterChange() }
    }

    /**
     * 一般告警筛选按钮（黄色）
     */
    private val normalFilterButton: JToggleButton = JToggleButton(GroupedScanResultPanel.Icons.WARNING).apply {
        toolTipText = "显示/隐藏一般告警"
        isBorderPainted = false
        isContentAreaFilled = false
        isSelected = true
        addActionListener { handleFilterChange() }
    }

    /**
     * 提示告警筛选按钮（蓝色）
     */
    private val infoFilterButton: JToggleButton = JToggleButton(GroupedScanResultPanel.Icons.NORMAL).apply {
        toolTipText = "显示/隐藏提示告警"
        isBorderPainted = false
        isContentAreaFilled = false
        isSelected = true
        addActionListener { handleFilterChange() }
    }

    /**
     * 工具栏（竖排按钮）
     * 
     * 注意：必须在所有按钮初始化之后创建
     */
    private val toolbarPanel: JPanel = createVerticalToolbarPanel()

    init {
        initializeLayout()
    }

    /**
     * 初始化面板布局
     */
    private fun initializeLayout() {
        // 创建扫描范围选择器面板
        val scopeSelectorPanel = createScopeSelectorPanel()

        // 创建结果视图面板（顶部范围选择器 + 中央共用结果面板）
        val resultsViewPanel = JBPanel<JBPanel<*>>(BorderLayout())
        resultsViewPanel.add(scopeSelectorPanel, BorderLayout.NORTH)
        resultsViewPanel.add(scanResultPanel.getContent(), BorderLayout.CENTER)

        // 添加两个视图到 CardLayout 面板
        contentPanel.add(resultsViewPanel, CARD_RESULTS)
        contentPanel.add(scanProgressPanel.getContent(), CARD_PROGRESS)

        // 设置扫描完成回调
        scanProgressPanel.setOnScanCompleteCallback {
            SwingUtilities.invokeLater {
                cardLayout.show(contentPanel, CARD_RESULTS)
                refresh()
            }
        }

        // 设置扫描取消回调（用户通过进度面板的"取消扫描"按钮取消时触发）
        scanProgressPanel.setOnScanCancelCallback {
            SwingUtilities.invokeLater {
                cardLayout.show(contentPanel, CARD_RESULTS)
                playButton.isEnabled = true
                stopButton.isEnabled = false
            }
        }

        // 添加竖排工具栏到左侧
        mainPanel.add(toolbarPanel, BorderLayout.WEST)
        
        // 添加内容到中央（使用 CardLayout 面板）
        mainPanel.add(contentPanel, BorderLayout.CENTER)
    }

    /**
     * 创建扫描范围选择器面板
     *
     * 包含"当前文件"、"变更文件"、"所有文件"三个互斥按钮，
     * 仅用于选择扫描范围，不影响结果展示。
     */
    private fun createScopeSelectorPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(2)))
        panel.border = JBUI.Borders.customLineBottom(JBColor.border())
        panel.add(currentFileButton)
        panel.add(changedFilesButton)
        panel.add(allFilesButton)
        return panel
    }

    /**
     * 获取当前选择的扫描范围索引
     */
    fun getSelectedScopeIndex(): Int {
        return when {
            currentFileButton.isSelected -> SCOPE_CURRENT_FILE
            changedFilesButton.isSelected -> SCOPE_CHANGED_FILES
            allFilesButton.isSelected -> SCOPE_ALL_FILES
            else -> SCOPE_CURRENT_FILE
        }
    }

    /**
     * 创建竖排工具栏面板
     *
     * @return 工具栏面板
     */
    private fun createVerticalToolbarPanel(): JPanel {
        // 使用 BorderLayout 包装，确保内容居中
        val outerPanel = JBPanel<JBPanel<*>>(BorderLayout())
        outerPanel.border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 0, 1)
        
        // 设置固定宽度
        outerPanel.preferredSize = Dimension(JBUI.scale(34), 0)
        outerPanel.minimumSize = Dimension(JBUI.scale(34), 0)
        outerPanel.maximumSize = Dimension(JBUI.scale(34), Int.MAX_VALUE)

        // 内部面板使用 BoxLayout 竖排按钮
        val buttonPanel = JBPanel<JBPanel<*>>()
        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.Y_AXIS)
        buttonPanel.isOpaque = false
        
        // 添加操作按钮（播放、停止、刷新、清除）
        listOf(playButton, stopButton, refreshButton, clearButton).forEach { button ->
            button.alignmentX = java.awt.Component.CENTER_ALIGNMENT
            buttonPanel.add(Box.createVerticalStrut(JBUI.scale(3)))
            buttonPanel.add(button)
        }
        
        // 添加分隔线
        buttonPanel.add(Box.createVerticalStrut(JBUI.scale(10)))
        val separator = JSeparator(SwingConstants.HORIZONTAL)
        separator.maximumSize = Dimension(JBUI.scale(24), JBUI.scale(1))
        separator.alignmentX = java.awt.Component.CENTER_ALIGNMENT
        buttonPanel.add(separator)
        buttonPanel.add(Box.createVerticalStrut(JBUI.scale(10)))
        
        // 添加筛选按钮（严重、一般、提示）
        listOf(criticalFilterButton, normalFilterButton, infoFilterButton).forEach { button ->
            button.alignmentX = java.awt.Component.CENTER_ALIGNMENT
            buttonPanel.add(button)
            buttonPanel.add(Box.createVerticalStrut(JBUI.scale(3)))
        }
        
        // 添加弹簧，将按钮推到顶部
        buttonPanel.add(Box.createVerticalGlue())
        
        // 将按钮面板放在外部面板的中央
        outerPanel.add(buttonPanel, BorderLayout.CENTER)

        return outerPanel
    }

    /**
     * 处理筛选条件变化
     */
    private fun handleFilterChange() {
        val showCritical = criticalFilterButton.isSelected
        val showNormal = normalFilterButton.isSelected
        val showInfo = infoFilterButton.isSelected
        
        logger.info("筛选条件变化: 严重=$showCritical, 一般=$showNormal, 提示=$showInfo")
        
        scanResultPanel.setShowCritical(showCritical)
        scanResultPanel.setShowNormal(showNormal)
        scanResultPanel.setShowInfo(showInfo)
    }

    /**
     * 处理启动扫描
     *
     * 根据当前选择的扫描范围按钮调用不同的扫描接口：
     * - 当前文件 → targetScan
     * - 变更文件 → preCommitScan
     * - 所有文件 → fullScan
     */
    @Suppress("TooGenericExceptionCaught")
    private fun handleStartScan() {
        val scopeIndex = getSelectedScopeIndex()
        logger.info("用户点击启动扫描按钮，扫描范围: $scopeIndex")

        playButton.isEnabled = false
        stopButton.isEnabled = true

        coroutineScope.launch {
            try {
                val scanService = ScanService.getInstance(project)
                
                val result = when (scopeIndex) {
                    SCOPE_CURRENT_FILE -> {
                        logger.info("执行当前文件扫描")
                        val currentFilePath = getCurrentFilePath()
                        if (currentFilePath != null) {
                            scanService.targetScan(listOf(currentFilePath))
                        } else {
                            logger.warn("没有打开的文件")
                            SwingUtilities.invokeLater {
                                playButton.isEnabled = true
                                stopButton.isEnabled = false
                            }
                            return@launch
                        }
                    }
                    SCOPE_CHANGED_FILES -> {
                        logger.info("执行变更文件扫描 (pre-commit)")
                        scanService.preCommitScan()
                    }
                    SCOPE_ALL_FILES -> {
                        logger.info("执行所有文件扫描 (全量)")
                        scanService.fullScan()
                    }
                    else -> {
                        logger.warn("未知的扫描范围: $scopeIndex")
                        SwingUtilities.invokeLater {
                            playButton.isEnabled = true
                            stopButton.isEnabled = false
                        }
                        return@launch
                    }
                }

                SwingUtilities.invokeLater {
                    when (result) {
                        is ScanResult.Success -> {
                            logger.info("扫描启动成功: ${result.response.message}")
                            cardLayout.show(contentPanel, CARD_PROGRESS)
                            scanProgressPanel.startPolling()
                        }
                        is ScanResult.Failure -> {
                            logger.error("扫描启动失败: ${result.message}", result.exception)
                            playButton.isEnabled = true
                            stopButton.isEnabled = false
                        }
                    }
                }

            } catch (e: Exception) {
                logger.error("启动扫描异常: ${e.message}", e)
                SwingUtilities.invokeLater {
                    playButton.isEnabled = true
                    stopButton.isEnabled = false
                }
            }
        }
    }

    /**
     * 获取当前打开文件的路径
     */
    private fun getCurrentFilePath(): String? {
        val fileEditorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
        val selectedFiles = fileEditorManager.selectedFiles
        return if (selectedFiles.isNotEmpty()) {
            selectedFiles[0].path
        } else {
            null
        }
    }

    /**
     * 处理停止扫描
     */
    @Suppress("TooGenericExceptionCaught")
    private fun handleStopScan() {
        logger.info("用户点击停止扫描按钮")
        
        coroutineScope.launch {
            try {
                val scanService = ScanService.getInstance(project)
                scanService.cancelScan()
                
                SwingUtilities.invokeLater {
                    // 停止进度轮询
                    scanProgressPanel.stopPolling()
                    // 切换回结果视图
                    cardLayout.show(contentPanel, CARD_RESULTS)
                    // 恢复按钮状态
                    playButton.isEnabled = true
                    stopButton.isEnabled = false
                }
            } catch (e: Exception) {
                logger.error("停止扫描异常: ${e.message}", e)
            }
        }
    }

    /**
     * 处理刷新结果
     */
    private fun handleRefreshResults() {
        logger.info("刷新扫描结果")
        
        scanResultPanel.loadScanResult()
        
        playButton.isEnabled = true
        stopButton.isEnabled = false
    }

    /**
     * 处理清除结果
     */
    private fun handleClearResults() {
        logger.info("清除结果")
        scanResultPanel.clear()
    }

    /**
     * 切换到扫描进度视图并启动轮询
     *
     * 供外部 Action（如右键菜单扫描、主菜单扫描）在扫描成功启动后调用，
     * 以在工具窗口中展示实时扫描进度。
     */
    fun showScanProgress() {
        cardLayout.show(contentPanel, CARD_PROGRESS)
        scanProgressPanel.startPolling()
        playButton.isEnabled = false
        stopButton.isEnabled = true
    }

    /**
     * 刷新
     */
    fun refresh() {
        handleRefreshResults()
    }

    /**
     * 清除
     */
    fun clear() {
        handleClearResults()
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
