package com.codecc.preci.ui.toolwindow

import com.codecc.preci.api.model.response.PipelineBuild
import com.codecc.preci.api.model.response.PipelineBuildLog
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.pipeline.BuildHistoryResult
import com.codecc.preci.service.pipeline.BuildLogsResult
import com.codecc.preci.service.pipeline.PipelineService
import com.codecc.preci.service.pipeline.StartBuildResult
import com.codecc.preci.service.pipeline.StopBuildResult
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.CancellationException
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

/**
 * PAC 流水线面板
 *
 * 提供蓝盾 PAC 流水线管理功能，包含：
 * - 触发构建：启动一次新的流水线构建
 * - 构建历史：以表格形式展示历史构建记录
 * - 构建日志：查看指定构建的实时日志
 * - 停止构建：停止正在运行的构建
 *
 * **布局结构：**
 * ```
 * +--------------------------------------------------+
 * | [> 触发构建] [R 刷新]                             | <- 工具栏
 * +------------------------+-------------------------+
 * | 构建号|时间|状态|操作   | 构建日志                 |
 * |-------+----+----+------| [12:00:00] Step 1 done   |
 * | #1    |... |成功|       | [12:00:01] Step 2 start  |
 * | #2    |... |运行|停止   |                          |
 * +------------------------+-------------------------+
 * ```
 *
 * @property project 当前项目
 * @property coroutineScope 协程作用域
 * @since 1.0
 */
class PacPipelinePanel(
    private val project: Project,
    private val coroutineScope: CoroutineScope
) {

    private val logger = PreCILogger.getLogger(PacPipelinePanel::class.java)

    /** Disposable，用于 JBLoadingPanel 生命周期管理 */
    private val disposable = Disposer.newDisposable("PacPipelinePanel")

    /** 加载面板，包裹主要内容区域 */
    private val loadingPanel = JBLoadingPanel(BorderLayout(), disposable, 300)

    /** 主面板容器 */
    private val mainPanel: JPanel = JBPanel<JBPanel<*>>(BorderLayout())

    // ========== 表格组件 ==========

    /** 构建历史表格模型 */
    private val tableModel = BuildHistoryTableModel()

    /** 构建历史表格 */
    private val buildTable = JBTable(tableModel).apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        rowHeight = JBUI.scale(28)
        tableHeader.reorderingAllowed = false
    }

    // ========== 日志组件 ==========

    /** 构建日志文本区域 */
    private val logTextArea = JTextArea().apply {
        isEditable = false
        font = JBUI.Fonts.create("Monospaced", 12)
        border = JBUI.Borders.empty(4)
    }

    /** 日志标题标签 */
    private val logTitleLabel = JLabel("构建日志").apply {
        border = JBUI.Borders.empty(4, 8)
        font = font.deriveFont(Font.BOLD)
    }

    // ========== 工具栏按钮 ==========

    /** 触发构建按钮 */
    private val startBuildButton = JButton("触发构建", AllIcons.Actions.Execute).apply {
        toolTipText = "触发一次新的流水线构建"
        addActionListener { handleStartBuild() }
    }

    /** 刷新按钮 */
    private val refreshButton = JButton("刷新", AllIcons.Actions.Refresh).apply {
        toolTipText = "刷新构建历史"
        addActionListener { handleRefresh() }
    }

    // ========== 状态 ==========

    /** 标记是否已通过 onTabActivated 执行过一次自动加载 */
    private var hasAutoLoaded = false

    /** 当前正在查看日志的 buildId */
    private var currentLogBuildId: String? = null

    /** 日志轮询协程 Job */
    private var logPollingJob: Job? = null

    companion object {
        private const val COL_BUILD_NUM = 0
        private const val COL_START_TIME = 1
        private const val COL_END_TIME = 2
        private const val COL_DURATION = 3
        private const val COL_STATUS = 4
        private const val COL_ACTION = 5

        private val TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())

        /** 日志轮询间隔（毫秒） */
        private const val LOG_POLL_INTERVAL_MS = 3000L

        /** 触发构建时，超过此时间（毫秒）未返回则弹出等待提示 */
        private const val WAIT_HINT_DELAY_MS = 60_000L
    }

    init {
        initializeLayout()
        setupTableListeners()
    }

    /**
     * 初始化面板布局
     */
    private fun initializeLayout() {
        val toolbarPanel = createToolbarPanel()

        val tableScrollPane = JBScrollPane(buildTable).apply {
            border = JBUI.Borders.empty()
        }

        val logHeaderPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(logTitleLabel, BorderLayout.WEST)
            border = JBUI.Borders.customLine(JBColor.border(), 0, 1, 0, 0)
        }
        val logScrollPane = JBScrollPane(logTextArea).apply {
            border = JBUI.Borders.empty()
        }
        val logPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(logHeaderPanel, BorderLayout.NORTH)
            add(logScrollPane, BorderLayout.CENTER)
        }

        val splitter = OnePixelSplitter(false, 0.45f).apply {
            firstComponent = tableScrollPane
            secondComponent = logPanel
        }

        setupTableColumns()

        loadingPanel.add(splitter, BorderLayout.CENTER)

        mainPanel.add(toolbarPanel, BorderLayout.NORTH)
        mainPanel.add(loadingPanel, BorderLayout.CENTER)
    }

    /**
     * 创建工具栏面板
     *
     * @return 工具栏面板
     */
    private fun createToolbarPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), JBUI.scale(4)))
        panel.border = JBUI.Borders.customLineBottom(JBColor.border())
        panel.add(startBuildButton)
        panel.add(refreshButton)
        return panel
    }

    /**
     * 设置表格列宽和渲染器
     */
    private fun setupTableColumns() {
        val columnModel = buildTable.columnModel
        columnModel.getColumn(COL_BUILD_NUM).preferredWidth = JBUI.scale(80)
        columnModel.getColumn(COL_START_TIME).preferredWidth = JBUI.scale(160)
        columnModel.getColumn(COL_END_TIME).preferredWidth = JBUI.scale(160)
        columnModel.getColumn(COL_DURATION).preferredWidth = JBUI.scale(80)
        columnModel.getColumn(COL_STATUS).preferredWidth = JBUI.scale(80)
        columnModel.getColumn(COL_ACTION).preferredWidth = JBUI.scale(60)

        columnModel.getColumn(COL_STATUS).cellRenderer = StatusColumnRenderer()
        columnModel.getColumn(COL_ACTION).cellRenderer = ActionColumnRenderer()
    }

    /**
     * 设置表格事件监听
     */
    private fun setupTableListeners() {
        buildTable.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val selectedRow = buildTable.selectedRow
                if (selectedRow >= 0) {
                    val build = tableModel.getBuild(selectedRow)
                    handleBuildSelected(build)
                }
            }
        }

        buildTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val row = buildTable.rowAtPoint(e.point)
                val col = buildTable.columnAtPoint(e.point)
                if (col == COL_ACTION && row >= 0) {
                    val build = tableModel.getBuild(row)
                    if (build.status == "RUNNING") {
                        handleStopBuild(build.buildId)
                    }
                }
            }
        })
    }

    // ========== 事件处理 ==========

    /**
     * 当 Tab 被激活时调用
     *
     * 首次激活时自动加载构建历史。
     */
    fun onTabActivated() {
        if (hasAutoLoaded) return
        hasAutoLoaded = true
        loadBuildHistory()
    }

    /**
     * 刷新构建历史
     */
    fun refresh() {
        loadBuildHistory()
    }

    /**
     * 加载构建历史
     */
    @Suppress("TooGenericExceptionCaught")
    private fun loadBuildHistory() {
        loadingPanel.startLoading()
        refreshButton.isEnabled = false

        coroutineScope.launch {
            try {
                val pipelineService = PipelineService.getInstance(project)
                val result = pipelineService.getBuildHistory()

                SwingUtilities.invokeLater {
                    loadingPanel.stopLoading()
                    refreshButton.isEnabled = true
                    when (result) {
                        is BuildHistoryResult.Success -> {
                            tableModel.updateBuilds(result.builds)
                            logger.info("构建历史已刷新，共 ${result.builds.size} 条记录")
                        }
                        is BuildHistoryResult.Failure -> {
                            logger.warn("加载构建历史失败: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("加载构建历史异常: ${e.message}", e)
                SwingUtilities.invokeLater {
                    loadingPanel.stopLoading()
                    refreshButton.isEnabled = true
                }
            }
        }
    }

    /**
     * 处理触发构建
     *
     * 构建触发成功后，刷新历史列表并自动开始轮询新构建的日志。
     * 若请求超过 1 分钟未返回，弹出通知提示用户耐心等待。
     */
    @Suppress("TooGenericExceptionCaught")
    private fun handleStartBuild() {
        logger.info("用户触发流水线构建")
        startBuildButton.isEnabled = false

        coroutineScope.launch {
            val waitHintJob = launch {
                delay(WAIT_HINT_DELAY_MS)
                showNotification(
                    "正在准备构建资源",
                    "首次构建需要安装 Agent 等准备工作，可能需要几分钟，请耐心等待...",
                    com.intellij.notification.NotificationType.INFORMATION
                )
            }

            try {
                val pipelineService = PipelineService.getInstance(project)
                val result = pipelineService.startBuild()

                waitHintJob.cancel()

                SwingUtilities.invokeLater {
                    startBuildButton.isEnabled = true
                    when (result) {
                        is StartBuildResult.Success -> {
                            logger.info("构建触发成功，buildId: ${result.buildId}")
                            onBuildTriggered(result.buildId)
                        }
                        is StartBuildResult.Failure -> {
                            logger.warn("触发构建失败: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                waitHintJob.cancel()
                logger.error("触发构建异常: ${e.message}", e)
                SwingUtilities.invokeLater {
                    startBuildButton.isEnabled = true
                }
            }
        }
    }

    /**
     * 构建触发成功后的处理：刷新历史列表，自动选中新构建并开始日志轮询
     *
     * @param buildId 新触发的构建 ID
     */
    private fun onBuildTriggered(buildId: String) {
        currentLogBuildId = buildId
        logPollingJob?.cancel()
        logTextArea.text = ""
        logTitleLabel.text = "构建日志 - 构建中..."

        startLogPolling(buildId)
        loadBuildHistoryAndSelect(buildId)
    }

    /**
     * 加载构建历史并自动选中指定 buildId 对应的行
     *
     * @param targetBuildId 要选中的构建 ID
     */
    @Suppress("TooGenericExceptionCaught")
    private fun loadBuildHistoryAndSelect(targetBuildId: String) {
        loadingPanel.startLoading()
        refreshButton.isEnabled = false

        coroutineScope.launch {
            try {
                val pipelineService = PipelineService.getInstance(project)
                val result = pipelineService.getBuildHistory()

                SwingUtilities.invokeLater {
                    loadingPanel.stopLoading()
                    refreshButton.isEnabled = true
                    when (result) {
                        is BuildHistoryResult.Success -> {
                            tableModel.updateBuilds(result.builds)
                            val idx = result.builds.indexOfFirst { it.buildId == targetBuildId }
                            if (idx >= 0) {
                                buildTable.selectionModel.setSelectionInterval(idx, idx)
                                logTitleLabel.text = "构建日志 - #${result.builds[idx].buildNum}"
                            }
                        }
                        is BuildHistoryResult.Failure -> {
                            logger.warn("加载构建历史失败: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("加载构建历史异常: ${e.message}", e)
                SwingUtilities.invokeLater {
                    loadingPanel.stopLoading()
                    refreshButton.isEnabled = true
                }
            }
        }
    }

    /**
     * 处理刷新
     */
    private fun handleRefresh() {
        logger.info("用户刷新构建历史")
        loadBuildHistory()
    }

    /**
     * 处理选中构建行
     *
     * 加载对应构建的日志。如果构建正在运行，开始轮询日志。
     *
     * @param build 选中的构建
     */
    private fun handleBuildSelected(build: PipelineBuild) {
        logPollingJob?.cancel()
        currentLogBuildId = build.buildId
        logTextArea.text = ""
        logTitleLabel.text = "构建日志 - #${build.buildNum}"

        if (build.status == "RUNNING") {
            startLogPolling(build.buildId)
        } else {
            fetchLogsOnce(build.buildId)
        }
    }

    /**
     * 一次性获取构建日志（用于已完成的构建）
     *
     * @param buildId 构建 ID
     */
    @Suppress("TooGenericExceptionCaught")
    private fun fetchLogsOnce(buildId: String) {
        coroutineScope.launch {
            try {
                val pipelineService = PipelineService.getInstance(project)
                val result = pipelineService.getBuildLogs(buildId)

                SwingUtilities.invokeLater {
                    if (currentLogBuildId == buildId) {
                        when (result) {
                            is BuildLogsResult.Success -> appendLogs(result.logs)
                            is BuildLogsResult.Failure -> {
                                logTextArea.append("获取日志失败: ${result.message}\n")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("获取构建日志异常: ${e.message}", e)
            }
        }
    }

    /**
     * 开始轮询构建日志（用于运行中的构建）
     *
     * 每隔 [LOG_POLL_INTERVAL_MS] 毫秒增量获取新日志，直到日志输出完毕或用户切换构建。
     *
     * @param buildId 构建 ID
     */
    private fun startLogPolling(buildId: String) {
        logPollingJob?.cancel()
        logPollingJob = coroutineScope.launch {
            var start = 0L
            while (isActive && currentLogBuildId == buildId) {
                try {
                    val pipelineService = PipelineService.getInstance(project)
                    val result = pipelineService.getBuildLogs(buildId, start)

                    when (result) {
                        is BuildLogsResult.Success -> {
                            if (result.logs.isNotEmpty()) {
                                SwingUtilities.invokeLater {
                                    if (currentLogBuildId == buildId) {
                                        appendLogs(result.logs)
                                    }
                                }
                                start = result.logs.last().lineNo + 1
                            }
                            if (result.finished) {
                                SwingUtilities.invokeLater { loadBuildHistory() }
                                break
                            }
                        }
                        is BuildLogsResult.Failure -> {
                            logger.warn("轮询日志失败: ${result.message}")
                            break
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("轮询日志异常: ${e.message}", e)
                    break
                }

                delay(LOG_POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * 追加日志内容到文本区域
     *
     * @param logs 日志行列表
     */
    private fun appendLogs(logs: List<PipelineBuildLog>) {
        val sb = StringBuilder()
        for (log in logs) {
            val timeStr = if (log.timestamp > 0) {
                TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(log.timestamp))
            } else {
                ""
            }
            if (timeStr.isNotEmpty()) {
                sb.append("[$timeStr] ")
            }
            sb.append(log.message).append('\n')
        }
        logTextArea.append(sb.toString())
        logTextArea.caretPosition = logTextArea.document.length
    }

    /**
     * 处理停止构建
     *
     * @param buildId 要停止的构建 ID
     */
    @Suppress("TooGenericExceptionCaught")
    private fun handleStopBuild(buildId: String) {
        logger.info("用户停止构建: $buildId")

        coroutineScope.launch {
            try {
                val pipelineService = PipelineService.getInstance(project)
                val result = pipelineService.stopBuild(buildId)

                SwingUtilities.invokeLater {
                    when (result) {
                        is StopBuildResult.Success -> {
                            logger.info("构建已停止: $buildId")
                            loadBuildHistory()
                        }
                        is StopBuildResult.Failure -> {
                            logger.warn("停止构建失败: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("停止构建异常: ${e.message}", e)
            }
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

    /**
     * 释放资源
     */
    fun dispose() {
        logPollingJob?.cancel()
        Disposer.dispose(disposable)
    }

    // ========== 内部类 ==========

    /**
     * 构建历史表格模型
     */
    private inner class BuildHistoryTableModel : AbstractTableModel() {

        private val columnNames = arrayOf("构建号", "开始时间", "结束时间", "耗时", "状态", "操作")
        private var builds: List<PipelineBuild> = emptyList()

        override fun getRowCount(): Int = builds.size
        override fun getColumnCount(): Int = columnNames.size
        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val build = builds[rowIndex]
            return when (columnIndex) {
                COL_BUILD_NUM -> "#${build.buildNum}"
                COL_START_TIME -> formatTimestamp(build.startTime)
                COL_END_TIME -> formatTimestamp(build.endTime)
                COL_DURATION -> formatDuration(build.startTime, build.endTime)
                COL_STATUS -> getStatusText(build.status)
                COL_ACTION -> if (build.status == "RUNNING") "停止" else ""
                else -> ""
            }
        }

        /**
         * 更新构建数据
         *
         * @param newBuilds 新的构建列表
         */
        fun updateBuilds(newBuilds: List<PipelineBuild>) {
            builds = newBuilds
            fireTableDataChanged()
        }

        /**
         * 获取指定行的构建数据
         *
         * @param row 行索引
         * @return 构建信息
         */
        fun getBuild(row: Int): PipelineBuild = builds[row]
    }

    /**
     * 状态列渲染器
     *
     * 根据构建状态显示不同颜色：成功-绿色、失败-红色、运行中-蓝色、已取消-灰色。
     */
    private inner class StatusColumnRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): java.awt.Component {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            if (!isSelected) {
                val build = tableModel.getBuild(row)
                foreground = when (build.status) {
                    "SUCCEED" -> JBColor(0x59A869, 0x499C54)
                    "FAILED" -> JBColor(0xDB5860, 0xC75450)
                    "RUNNING" -> JBColor(0x4083C9, 0x4A88C7)
                    "CANCELED" -> JBColor.GRAY
                    else -> JBColor.foreground()
                }
            }
            return this
        }
    }

    /**
     * 操作列渲染器
     *
     * 运行中的构建显示蓝色"停止"操作文本。
     */
    private inner class ActionColumnRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): java.awt.Component {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            val build = tableModel.getBuild(row)
            if (build.status == "RUNNING") {
                foreground = JBColor(0x4083C9, 0x4A88C7)
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            } else {
                text = ""
                foreground = JBColor.foreground()
            }
            return this
        }
    }

    // ========== 工具方法 ==========

    /**
     * 格式化时间戳为可读字符串
     *
     * @param timestamp Unix 时间戳（毫秒）
     * @return 格式化的时间字符串，时间戳为 0 时返回 "-"
     */
    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0) return "-"
        return TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(timestamp))
    }

    /**
     * 格式化耗时
     *
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @return 格式化的耗时字符串
     */
    private fun formatDuration(startTime: Long, endTime: Long): String {
        if (startTime <= 0 || endTime <= 0) return "-"
        val durationMs = endTime - startTime
        if (durationMs < 0) return "-"
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /**
     * 获取构建状态的中文描述
     *
     * @param status 构建状态英文标识
     * @return 中文状态文本
     */
    private fun getStatusText(status: String): String {
        return when (status) {
            "SUCCEED" -> "成功"
            "FAILED" -> "失败"
            "RUNNING" -> "运行中"
            "CANCELED" -> "已取消"
            "QUEUE" -> "排队中"
            else -> status
        }
    }

    /**
     * 显示 IDE 通知
     *
     * @param title 通知标题
     * @param content 通知内容
     * @param type 通知类型
     */
    @Suppress("TooGenericExceptionCaught")
    private fun showNotification(
        title: String,
        content: String,
        type: com.intellij.notification.NotificationType
    ) {
        try {
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("PreCI.Pipeline")
                .createNotification(title, content, type)
                .notify(project)
        } catch (e: Exception) {
            logger.error("显示通知失败: ${e.message}", e)
        }
    }
}
