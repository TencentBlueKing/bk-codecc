package com.codecc.preci.ui.toolwindow

import com.codecc.preci.api.model.response.Defect
import com.codecc.preci.api.model.response.RemoteDefectListResponse
import com.codecc.preci.api.model.response.RemoteTaskInfo
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.codecc.CodeCCService
import com.codecc.preci.util.PathHelper
import com.codecc.preci.service.codecc.RemoteDefectListResult
import com.codecc.preci.service.codecc.RemoteTaskListResult
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JTextField
import javax.swing.JToggleButton
import javax.swing.JTree
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

/**
 * 云端代码检查面板
 *
 * 提供与 CodeCC 平台的远程代码检查功能，包含：
 * - 任务选择：从 CodeCC 平台获取任务列表
 * - 维度过滤：按代码缺陷维度过滤
 * - 规则过滤：按检查规则过滤
 * - 缺陷展示：以树形结构展示远程缺陷（与本地代码检查风格一致）
 *
 * **布局结构：**
 * ```
 * ┌────────────────────────────────────────────────────┐
 * │ ▶ │ 任务: [下拉框]  维度: [下拉框]  规则: [输入框] │ ← 筛选栏
 * │ ↻ │────────────────────────────────────────────────│
 * │ 🗑 │ CodeCC found N issues ... (统计信息)          │
 * │   │────────────────────────────────────────────────│
 * │───│ ▼ FileName1.kt: 3 issues                      │
 * │ 🔴│   ├─ 🔴 缺陷描述 (行号) [工具名#规则名]       │
 * │ 🟡│   ├─ 🟡 缺陷描述 (行号) [工具名#规则名]       │
 * │ 🔵│   └─ 🔵 缺陷描述 (行号) [工具名#规则名]       │
 * │   │ ▶ FileName2.go: 10 issues                     │
 * └────────────────────────────────────────────────────┘
 * ```
 *
 * @property project 当前项目
 * @property coroutineScope 协程作用域
 * @since 1.0
 */
class CloudCodeCheckPanel(
    private val project: Project,
    private val coroutineScope: CoroutineScope
) {

    private val logger = PreCILogger.getLogger(CloudCodeCheckPanel::class.java)

    /** 主面板 */
    private val mainPanel: JPanel = JBPanel<JBPanel<*>>(BorderLayout())

    /** 标记任务列表是否正在加载，防止重复请求 */
    @Volatile
    private var isLoadingTasks = false

    /** 上次加载失败的时间戳，失败后 10 秒内不再自动重试 */
    @Volatile
    private var lastTaskLoadFailureTime = 0L

    /** 标记是否正在程序化更新下拉框，跳过搜索过滤逻辑 */
    private var isUpdatingComboBox = false

    /** 标记是否已通过 onTabActivated 执行过一次自动查询 */
    private var hasAutoQueried = false

    companion object {
        private const val TASK_LOAD_RETRY_COOLDOWN_MS = 10_000L

        private const val PREF_LAST_TASK_ID = "preci.cloud.lastTaskId"
        private const val PREF_LAST_DIM_DEFECT = "preci.cloud.dim.defect"
        private const val PREF_LAST_DIM_STANDARD = "preci.cloud.dim.standard"
        private const val PREF_LAST_DIM_SECURITY = "preci.cloud.dim.security"
        private const val PREF_LAST_CHECKER = "preci.cloud.lastChecker"
    }

    /** 全量任务列表 */
    private var allTasks: List<RemoteTaskInfo> = emptyList()

    /** 当前选中的任务 */
    private var selectedTask: RemoteTaskInfo? = null

    /** 任务显示按钮（模拟下拉框外观，点击弹出自定义搜索弹窗） */
    private val taskComboBox: JComboBox<RemoteTaskInfo> = JComboBox<RemoteTaskInfo>().apply {
        val fixedWidth = JBUI.scale(280)
        val fixedHeight = JBUI.scale(28)
        minimumSize = Dimension(fixedWidth, fixedHeight)
        preferredSize = Dimension(fixedWidth, fixedHeight)
        maximumSize = Dimension(fixedWidth, fixedHeight)
        renderer = TaskComboBoxRenderer()
    }

    /** 自定义任务搜索弹窗 */
    private var taskSearchPopup: JBPopup? = null
    private val taskSearchField: JTextField = JTextField()
    private val taskListModel: DefaultListModel<RemoteTaskInfo> = DefaultListModel()
    private val taskJList: JList<RemoteTaskInfo> = JList(taskListModel)

    /** 维度多选下拉框选项定义：显示名 → API 值 */
    private data class DimensionOption(val label: String, val value: String, var selected: Boolean = true) {
        override fun toString(): String = label
    }

    /** 维度选项列表 */
    private val dimensionOptions = listOf(
        DimensionOption("代码缺陷", "DEFECT"),
        DimensionOption("代码规范", "STANDARD"),
        DimensionOption("安全漏洞", "SECURITY")
    )

    /** 维度多选下拉框 */
    private val dimensionComboBox: JComboBox<String> = createDimensionComboBox()

    /** 规则下拉框 */
    private val checkerComboBox: JComboBox<String> = JComboBox<String>().apply {
        minimumSize = Dimension(JBUI.scale(120), JBUI.scale(28))
        toolTipText = "选择规则进行过滤"
    }

    /** 全量规则列表（"工具名#规则名" 格式） */
    private var allCheckers: List<String> = emptyList()

    /** 当前选中的规则 */
    private var selectedChecker: String? = null

    /** 自定义规则搜索弹窗 */
    private var checkerSearchPopup: JBPopup? = null
    private val checkerSearchField: JTextField = JTextField()
    private val checkerListModel: DefaultListModel<String> = DefaultListModel()
    private val checkerJList: JList<String> = JList(checkerListModel)

    /** 统计信息标签 */
    private val summaryLabel: JLabel = JLabel(" ").apply {
        border = JBUI.Borders.empty(4, 8)
    }

    /** 树形根节点 */
    private val rootNode = DefaultMutableTreeNode("远程检查结果")

    /** 树模型 */
    private val treeModel = DefaultTreeModel(rootNode)

    /** 树形组件 */
    private val defectTree: Tree = Tree(treeModel).apply {
        isRootVisible = false
        showsRootHandles = true
        rowHeight = 24
        cellRenderer = RemoteDefectTreeCellRenderer()
        addTreeSelectionListener { navigateToSelectedDefect() }
    }

    /** 筛选器状态 */
    private var showCritical = true
    private var showNormal = true
    private var showInfo = true

    /** 当前显示的缺陷列表（未过滤） */
    private var currentDefects: List<Defect> = emptyList()

    /** 当前选中的任务列表（缓存） */
    private var cachedTasks: List<RemoteTaskInfo> = emptyList()

    // ========== 工具栏按钮 ==========

    /** 查询按钮 */
    private val queryButton: JButton = JButton(AllIcons.Actions.Execute).apply {
        toolTipText = "查询缺陷"
        isBorderPainted = false
        isContentAreaFilled = false
        addActionListener { handleQueryDefects() }
    }

    /** 刷新按钮 */
    private val refreshButton: JButton = JButton(AllIcons.Actions.Refresh).apply {
        toolTipText = "刷新任务列表"
        isBorderPainted = false
        isContentAreaFilled = false
        addActionListener { handleRefreshTasks() }
    }

    /** 清除按钮 */
    private val clearButton: JButton = JButton(AllIcons.Actions.GC).apply {
        toolTipText = "清除结果"
        isBorderPainted = false
        isContentAreaFilled = false
        addActionListener { handleClear() }
    }

    /** 严重告警筛选按钮 */
    private val criticalFilterButton: JToggleButton = JToggleButton(GroupedScanResultPanel.Icons.ERROR).apply {
        toolTipText = "显示/隐藏严重告警"
        isBorderPainted = false
        isContentAreaFilled = false
        isSelected = true
        addActionListener { handleFilterChange() }
    }

    /** 一般告警筛选按钮 */
    private val normalFilterButton: JToggleButton = JToggleButton(GroupedScanResultPanel.Icons.WARNING).apply {
        toolTipText = "显示/隐藏一般告警"
        isBorderPainted = false
        isContentAreaFilled = false
        isSelected = true
        addActionListener { handleFilterChange() }
    }

    /** 提示告警筛选按钮 */
    private val infoFilterButton: JToggleButton = JToggleButton(GroupedScanResultPanel.Icons.NORMAL).apply {
        toolTipText = "显示/隐藏提示告警"
        isBorderPainted = false
        isContentAreaFilled = false
        isSelected = true
        addActionListener { handleFilterChange() }
    }

    init {
        setupTaskSearchPopup()
        setupCheckerSearchPopup()
        initializeLayout()
    }

    /**
     * 初始化面板布局
     */
    private fun initializeLayout() {
        // 创建顶部筛选栏
        val filterPanel = createFilterPanel()

        // 创建统计信息面板
        val summaryPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)
            add(summaryLabel, BorderLayout.CENTER)
        }

        // 顶部容器（筛选栏 + 统计信息）
        val topContainer = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(filterPanel)
            add(summaryPanel)
        }

        // 创建树形滚动面板
        val scrollPane = JBScrollPane(defectTree).apply {
            border = JBUI.Borders.empty()
        }

        // 创建内容区域（顶部筛选 + 树形结果）
        val contentPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(topContainer, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }

        // 创建工具栏
        val toolbarPanel = createVerticalToolbarPanel()

        // 组装主面板
        mainPanel.add(toolbarPanel, BorderLayout.WEST)
        mainPanel.add(contentPanel, BorderLayout.CENTER)
    }

    /**
     * 创建维度多选下拉框
     *
     * 下拉列表中每项是一个 JCheckBox，点击切换勾选状态而不关闭弹窗。
     * 下拉框标题显示已选维度的摘要文本。
     */
    private fun createDimensionComboBox(): JComboBox<String> {
        val combo = object : JComboBox<String>() {
            // 阻止默认选中行为，让我们自己处理点击
            override fun setSelectedIndex(anIndex: Int) {
                // 不调用 super，防止 JComboBox 内部修改选中项
            }
        }
        combo.preferredSize = Dimension(JBUI.scale(140), JBUI.scale(28))

        // 添加占位 item（仅用于撑开下拉列表行数）
        dimensionOptions.forEach { combo.addItem(it.label) }

        combo.renderer = ListCellRenderer { _, value, index, _, _ ->
            if (index == -1) {
                // 折叠态：显示摘要文本
                JLabel(getDimensionSummaryText()).apply {
                    border = JBUI.Borders.empty(2, 4)
                }
            } else {
                // 展开态：显示带勾选框的选项
                val option = dimensionOptions.getOrNull(index)
                JCheckBox(value, option?.selected ?: false).apply {
                    isOpaque = true
                    border = JBUI.Borders.empty(2, 4)
                }
            }
        }

        // 点击切换勾选状态
        combo.addActionListener {
            val idx = combo.getSelectedIndex()
            if (idx in dimensionOptions.indices) {
                // getSelectedIndex 因为我们 override 了 setSelectedIndex 不会改变，
                // 需要通过 UI 内部的 list 获取真实点击的 index
            }
        }

        // 用 PopupMenuListener + 鼠标监听实现勾选切换
        combo.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                // 在 popup 可见后，给内部 JList 添加鼠标监听
                SwingUtilities.invokeLater {
                    val popup = combo.ui?.getAccessibleChild(combo, 0)
                    if (popup is javax.swing.plaf.basic.ComboPopup) {
                        val list = popup.list
                        // 移除旧监听器（防止重复添加）
                        list.mouseListeners
                            .filterIsInstance<DimensionCheckMouseListener>()
                            .forEach { list.removeMouseListener(it) }
                        list.addMouseListener(DimensionCheckMouseListener(combo, list))
                    }
                }
            }
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {}
            override fun popupMenuCanceled(e: PopupMenuEvent) {}
        })

        return combo
    }

    /** 获取维度下拉框折叠态的摘要文本 */
    private fun getDimensionSummaryText(): String {
        val selected = dimensionOptions.filter { it.selected }
        return when {
            selected.isEmpty() || selected.size == dimensionOptions.size -> "所有维度"
            else -> selected.joinToString(", ") { it.label }
        }
    }

    /** 刷新维度下拉框的显示文本 */
    private fun updateDimensionComboBoxText() {
        dimensionComboBox.repaint()
    }

    /**
     * 维度列表的鼠标监听器，点击时切换勾选而不关闭弹窗
     */
    private inner class DimensionCheckMouseListener(
        private val combo: JComboBox<String>,
        private val list: JList<*>
    ) : java.awt.event.MouseAdapter() {
        override fun mousePressed(e: java.awt.event.MouseEvent) {
            val idx = list.locationToIndex(e.point)
            if (idx in dimensionOptions.indices) {
                dimensionOptions[idx].selected = !dimensionOptions[idx].selected
                list.repaint()
                combo.repaint()
                // 消费事件，阻止弹窗关闭
                e.consume()
            }
        }
    }

    /**
     * 创建筛选栏面板
     *
     * @return 筛选栏面板
     */
    private fun createFilterPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), JBUI.scale(4)))
        panel.border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)

        panel.add(JLabel("任务:"))
        panel.add(taskComboBox)
        panel.add(JLabel("维度:"))
        panel.add(dimensionComboBox)
        panel.add(JLabel("规则:"))
        panel.add(checkerComboBox)

        return panel
    }

    /**
     * 创建竖排工具栏面板
     *
     * @return 工具栏面板
     */
    private fun createVerticalToolbarPanel(): JPanel {
        val outerPanel = JBPanel<JBPanel<*>>(BorderLayout())
        outerPanel.border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 0, 1)
        outerPanel.preferredSize = Dimension(JBUI.scale(34), 0)
        outerPanel.minimumSize = Dimension(JBUI.scale(34), 0)
        outerPanel.maximumSize = Dimension(JBUI.scale(34), Int.MAX_VALUE)

        val buttonPanel = JBPanel<JBPanel<*>>()
        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.Y_AXIS)
        buttonPanel.isOpaque = false

        listOf(queryButton, refreshButton, clearButton).forEach { button ->
            button.alignmentX = java.awt.Component.CENTER_ALIGNMENT
            buttonPanel.add(Box.createVerticalStrut(JBUI.scale(3)))
            buttonPanel.add(button)
        }

        // 分隔线
        buttonPanel.add(Box.createVerticalStrut(JBUI.scale(10)))
        val separator = JSeparator(SwingConstants.HORIZONTAL)
        separator.maximumSize = Dimension(JBUI.scale(24), JBUI.scale(1))
        separator.alignmentX = java.awt.Component.CENTER_ALIGNMENT
        buttonPanel.add(separator)
        buttonPanel.add(Box.createVerticalStrut(JBUI.scale(10)))

        listOf(criticalFilterButton, normalFilterButton, infoFilterButton).forEach { button ->
            button.alignmentX = java.awt.Component.CENTER_ALIGNMENT
            buttonPanel.add(button)
            buttonPanel.add(Box.createVerticalStrut(JBUI.scale(3)))
        }

        buttonPanel.add(Box.createVerticalGlue())
        outerPanel.add(buttonPanel, BorderLayout.CENTER)

        return outerPanel
    }

    // ========== 事件处理 ==========

    /**
     * 下拉框展开时自动加载任务列表
     *
     * 防重复机制：
     * - 正在加载中 → 跳过
     * - 已有缓存 → 跳过
     * - 上次加载失败未超过冷却时间 → 跳过（避免密集触发 OAuth 登录）
     */
    @Suppress("TooGenericExceptionCaught")
    private fun loadTasksOnDropdownOpen() {
        if (isLoadingTasks) return
        if (cachedTasks.isNotEmpty()) return

        val timeSinceLastFailure = System.currentTimeMillis() - lastTaskLoadFailureTime
        if (lastTaskLoadFailureTime > 0 && timeSinceLastFailure < TASK_LOAD_RETRY_COOLDOWN_MS) {
            logger.debug("距离上次加载失败仅 ${timeSinceLastFailure}ms，冷却中跳过")
            return
        }

        isLoadingTasks = true
        logger.info("任务下拉框展开，开始加载任务列表")

        coroutineScope.launch {
            try {
                val codeccService = CodeCCService.getInstance(project)
                val result = codeccService.getRemoteTaskList()

                SwingUtilities.invokeLater {
                    isLoadingTasks = false
                    when (result) {
                        is RemoteTaskListResult.Success -> {
                            lastTaskLoadFailureTime = 0
                            updateTaskComboBox(result.tasks)
                        }
                        is RemoteTaskListResult.Failure -> {
                            lastTaskLoadFailureTime = System.currentTimeMillis()
                            logger.warn("加载任务列表失败: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("加载任务列表异常: ${e.message}", e)
                SwingUtilities.invokeLater {
                    isLoadingTasks = false
                    lastTaskLoadFailureTime = System.currentTimeMillis()
                }
            }
        }
    }

    /**
     * 处理查询缺陷
     */
    @Suppress("TooGenericExceptionCaught")
    private fun handleQueryDefects() {
        val selectedTask = this.selectedTask
        if (selectedTask == null) {
            showMessage("请先选择一个任务")
            return
        }

        logger.info("用户点击查询缺陷，任务: ${selectedTask.getDisplayName()}")
        queryButton.isEnabled = false

        coroutineScope.launch {
            try {
                val codeccService = CodeCCService.getInstance(project)

                val dimensionList = getDimensionFilter()
                val checker = selectedChecker?.substringAfter("#", "")?.takeIf { it.isNotBlank() }

                val result = codeccService.getRemoteDefectList(
                    taskId = selectedTask.taskId,
                    dimensionList = dimensionList,
                    checker = checker
                )

                SwingUtilities.invokeLater {
                    queryButton.isEnabled = true
                    when (result) {
                        is RemoteDefectListResult.Success -> {
                            displayDefectResult(result.response)
                            saveConfig()
                        }
                        is RemoteDefectListResult.Failure -> {
                            showErrorMessage("查询失败: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("查询缺陷异常: ${e.message}", e)
                SwingUtilities.invokeLater {
                    queryButton.isEnabled = true
                    showErrorMessage("查询失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 获取维度过滤条件
     *
     * 根据复选框状态构建维度列表。全选或全不选时返回 null（查询所有维度）。
     */
    private fun getDimensionFilter(): List<String>? {
        val selected = dimensionOptions.filter { it.selected }.map { it.value }
        return if (selected.isEmpty() || selected.size == dimensionOptions.size) null else selected
    }

    /**
     * 展示缺陷查询结果
     *
     * @param response 缺陷列表响应
     */
    private fun displayDefectResult(response: RemoteDefectListResponse) {
        val taskDisplay = selectedTask?.getDisplayName() ?: "unknown"
        summaryLabel.text = "CodeCC found ${response.totalCount} issues in task \"$taskDisplay\". " +
            "(${response.getSummaryText()})"

        val defects = response.getDefectList().map { it.toLocalDefect() }
        showDefects(defects)

        // 从缺陷数据中提取规则列表并更新规则下拉框
        val checkers = response.getDefectList()
            .map { "${it.toolName}#${it.checker}" }
            .filter { it != "#" }
            .distinct()
            .sorted()
        updateCheckerComboBox(checkers)

        logger.info("展示远程缺陷结果: ${defects.size} 个缺陷, ${checkers.size} 个规则")
    }

    /**
     * 处理刷新任务列表（强制清空缓存和冷却状态并重新获取）
     */
    @Suppress("TooGenericExceptionCaught")
    private fun handleRefreshTasks() {
        logger.info("用户点击刷新任务列表")
        cachedTasks = emptyList()
        lastTaskLoadFailureTime = 0
        refreshButton.isEnabled = false

        coroutineScope.launch {
            try {
                val codeccService = CodeCCService.getInstance(project)
                val result = codeccService.getRemoteTaskList()

                SwingUtilities.invokeLater {
                    refreshButton.isEnabled = true
                    when (result) {
                        is RemoteTaskListResult.Success -> {
                            updateTaskComboBox(result.tasks)
                        }
                        is RemoteTaskListResult.Failure -> {
                            showErrorMessage("获取任务列表失败: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("刷新任务列表异常: ${e.message}", e)
                SwingUtilities.invokeLater {
                    refreshButton.isEnabled = true
                    showErrorMessage("获取任务列表失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 更新任务下拉框
     *
     * @param tasks 任务列表
     * @param selectTaskId 可选，指定要选中的任务 ID（用于恢复历史配置）
     */
    private fun updateTaskComboBox(tasks: List<RemoteTaskInfo>, selectTaskId: Long? = null) {
        cachedTasks = tasks
        allTasks = tasks

        isUpdatingComboBox = true
        taskComboBox.removeAllItems()
        tasks.forEach { task -> taskComboBox.addItem(task) }

        if (selectTaskId != null) {
            val target = tasks.indexOfFirst { it.taskId == selectTaskId }
            if (target >= 0) {
                taskComboBox.selectedIndex = target
                selectedTask = tasks[target]
            }
        } else {
            taskComboBox.selectedIndex = -1
        }

        isUpdatingComboBox = false

        if (taskSearchPopup?.isVisible == true) {
            filterTaskList(taskSearchField.text.trim())
        }

        logger.info("任务下拉框已更新，共 ${tasks.size} 个任务")
    }

    /**
     * 初始化自定义任务搜索弹窗的组件（搜索框 + 列表）。
     * 弹窗实例在每次打开时通过 JBPopupFactory 创建。
     */
    private fun setupTaskSearchPopup() {
        taskSearchField.apply {
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
                JBUI.Borders.empty(4, 6)
            )

            document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) = onSearch()
                override fun removeUpdate(e: DocumentEvent) = onSearch()
                override fun changedUpdate(e: DocumentEvent) = onSearch()
                private fun onSearch() = filterTaskList(text.trim())
            })

            addKeyListener(object : java.awt.event.KeyAdapter() {
                override fun keyPressed(e: java.awt.event.KeyEvent) {
                    when (e.keyCode) {
                        java.awt.event.KeyEvent.VK_ESCAPE -> taskSearchPopup?.cancel()
                        java.awt.event.KeyEvent.VK_ENTER -> {
                            if (taskListModel.size() > 0) {
                                selectTaskFromPopup(taskListModel.getElementAt(0))
                            }
                        }
                        java.awt.event.KeyEvent.VK_DOWN -> {
                            taskJList.requestFocusInWindow()
                            if (taskJList.selectedIndex < 0 && taskListModel.size() > 0) {
                                taskJList.selectedIndex = 0
                            }
                        }
                    }
                }
            })
        }

        taskJList.apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            cellRenderer = TaskListCellRenderer()
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    val task = taskJList.selectedValue
                    if (task != null) selectTaskFromPopup(task)
                }
            })
            addKeyListener(object : java.awt.event.KeyAdapter() {
                override fun keyPressed(e: java.awt.event.KeyEvent) {
                    when (e.keyCode) {
                        java.awt.event.KeyEvent.VK_ENTER -> {
                            val task = taskJList.selectedValue
                            if (task != null) selectTaskFromPopup(task)
                        }
                        java.awt.event.KeyEvent.VK_ESCAPE -> taskSearchPopup?.cancel()
                    }
                }
            })
        }

        // 拦截 JComboBox 的默认弹窗，改为显示自定义弹窗
        taskComboBox.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                SwingUtilities.invokeLater {
                    taskComboBox.hidePopup()
                    showTaskSearchPopup()
                }
            }
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {}
            override fun popupMenuCanceled(e: PopupMenuEvent) {}
        })
    }

    /** 显示任务搜索弹窗（每次重新创建 JBPopup 实例） */
    private fun showTaskSearchPopup() {
        loadTasksOnDropdownOpen()

        taskSearchField.text = ""
        filterTaskList("")

        val scrollPane = JBScrollPane(taskJList).apply {
            border = JBUI.Borders.empty()
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

        val popupWidth = taskComboBox.width
        val contentPanel = JPanel(BorderLayout()).apply {
            add(taskSearchField, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
            preferredSize = Dimension(popupWidth, JBUI.scale(250))
        }

        taskSearchPopup?.cancel()
        taskSearchPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(contentPanel, taskSearchField)
            .setRequestFocus(true)
            .setFocusable(true)
            .setMovable(false)
            .setResizable(false)
            .setCancelOnClickOutside(true)
            .setCancelOnWindowDeactivation(true)
            .setCancelKeyEnabled(true)
            .createPopup()

        taskSearchPopup?.showUnderneathOf(taskComboBox)
    }

    /** 从弹窗中选中一个任务 */
    private fun selectTaskFromPopup(task: RemoteTaskInfo) {
        selectedTask = task
        taskSearchPopup?.cancel()

        isUpdatingComboBox = true
        taskComboBox.removeAllItems()
        allTasks.forEach { taskComboBox.addItem(it) }
        val idx = allTasks.indexOfFirst { it.taskId == task.taskId }
        if (idx >= 0) taskComboBox.selectedIndex = idx
        isUpdatingComboBox = false
    }

    /** 根据关键字过滤弹窗中的任务列表 */
    private fun filterTaskList(keyword: String) {
        taskListModel.clear()
        val filtered = if (keyword.isBlank()) {
            allTasks
        } else {
            val lower = keyword.lowercase()
            allTasks.filter {
                it.nameCn.lowercase().contains(lower) ||
                    it.nameEn.lowercase().contains(lower)
            }
        }
        filtered.forEach { taskListModel.addElement(it) }
    }

    /** 任务列表的单元格渲染器 */
    private class TaskListCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): java.awt.Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is RemoteTaskInfo) {
                text = value.getDisplayName()
            }
            return this
        }
    }

    // ==================== 规则搜索弹窗 ====================

    /** 初始化规则搜索弹窗组件 */
    private fun setupCheckerSearchPopup() {
        checkerSearchField.apply {
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
                JBUI.Borders.empty(4, 6)
            )

            document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) = onSearch()
                override fun removeUpdate(e: DocumentEvent) = onSearch()
                override fun changedUpdate(e: DocumentEvent) = onSearch()
                private fun onSearch() = filterCheckerList(text.trim())
            })

            addKeyListener(object : java.awt.event.KeyAdapter() {
                override fun keyPressed(e: java.awt.event.KeyEvent) {
                    when (e.keyCode) {
                        java.awt.event.KeyEvent.VK_ESCAPE -> checkerSearchPopup?.cancel()
                        java.awt.event.KeyEvent.VK_ENTER -> {
                            if (checkerListModel.size() > 0) {
                                selectCheckerFromPopup(checkerListModel.getElementAt(0))
                            }
                        }
                        java.awt.event.KeyEvent.VK_DOWN -> {
                            checkerJList.requestFocusInWindow()
                            if (checkerJList.selectedIndex < 0 && checkerListModel.size() > 0) {
                                checkerJList.selectedIndex = 0
                            }
                        }
                    }
                }
            })
        }

        checkerJList.apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    val checker = checkerJList.selectedValue
                    if (checker != null) selectCheckerFromPopup(checker)
                }
            })
            addKeyListener(object : java.awt.event.KeyAdapter() {
                override fun keyPressed(e: java.awt.event.KeyEvent) {
                    when (e.keyCode) {
                        java.awt.event.KeyEvent.VK_ENTER -> {
                            val checker = checkerJList.selectedValue
                            if (checker != null) selectCheckerFromPopup(checker)
                        }
                        java.awt.event.KeyEvent.VK_ESCAPE -> checkerSearchPopup?.cancel()
                    }
                }
            })
        }

        checkerComboBox.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                SwingUtilities.invokeLater {
                    checkerComboBox.hidePopup()
                    showCheckerSearchPopup()
                }
            }
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {}
            override fun popupMenuCanceled(e: PopupMenuEvent) {}
        })
    }

    /** 显示规则搜索弹窗 */
    private fun showCheckerSearchPopup() {
        checkerSearchField.text = ""
        filterCheckerList("")

        val scrollPane = JBScrollPane(checkerJList).apply {
            border = JBUI.Borders.empty()
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

        val popupWidth = checkerComboBox.width.coerceAtLeast(JBUI.scale(250))
        val contentPanel = JPanel(BorderLayout()).apply {
            add(checkerSearchField, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
            preferredSize = Dimension(popupWidth, JBUI.scale(250))
        }

        checkerSearchPopup?.cancel()
        checkerSearchPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(contentPanel, checkerSearchField)
            .setRequestFocus(true)
            .setFocusable(true)
            .setMovable(false)
            .setResizable(false)
            .setCancelOnClickOutside(true)
            .setCancelOnWindowDeactivation(true)
            .setCancelKeyEnabled(true)
            .createPopup()

        checkerSearchPopup?.showUnderneathOf(checkerComboBox)
    }

    /** 从规则弹窗中选中一个规则，立即过滤展示 */
    private fun selectCheckerFromPopup(checker: String) {
        selectedChecker = if (checker == "所有规则") null else checker
        checkerSearchPopup?.cancel()

        isUpdatingComboBox = true
        checkerComboBox.removeAllItems()
        checkerComboBox.addItem("所有规则")
        allCheckers.forEach { checkerComboBox.addItem(it) }
        if (selectedChecker != null) {
            val idx = allCheckers.indexOf(selectedChecker)
            if (idx >= 0) checkerComboBox.selectedIndex = idx + 1
        } else {
            checkerComboBox.selectedIndex = 0
        }
        isUpdatingComboBox = false

        rebuildTree()
    }

    /** 根据关键字过滤规则列表 */
    private fun filterCheckerList(keyword: String) {
        checkerListModel.clear()
        // 第一项始终为"所有规则"
        checkerListModel.addElement("所有规则")
        val filtered = if (keyword.isBlank()) {
            allCheckers
        } else {
            val lower = keyword.lowercase()
            allCheckers.filter { it.lowercase().contains(lower) }
        }
        filtered.forEach { checkerListModel.addElement(it) }
    }

    /** 更新规则下拉框的可选列表 */
    private fun updateCheckerComboBox(checkers: List<String>) {
        allCheckers = checkers

        isUpdatingComboBox = true
        checkerComboBox.removeAllItems()
        checkerComboBox.addItem("所有规则")
        checkers.forEach { checkerComboBox.addItem(it) }

        // 恢复之前的选中项
        if (selectedChecker != null) {
            val idx = checkers.indexOf(selectedChecker)
            if (idx >= 0) {
                checkerComboBox.selectedIndex = idx + 1
            } else {
                checkerComboBox.selectedIndex = 0
                selectedChecker = null
            }
        } else {
            checkerComboBox.selectedIndex = 0
        }
        isUpdatingComboBox = false
    }

    // ========== 配置持久化 ==========

    /**
     * 保存当前筛选配置到项目级持久化存储
     */
    private fun saveConfig() {
        val props = PropertiesComponent.getInstance(project)
        if (selectedTask != null) {
            props.setValue(PREF_LAST_TASK_ID, selectedTask!!.taskId.toString())
        }
        props.setValue(PREF_LAST_DIM_DEFECT, dimensionOptions[0].selected.toString())
        props.setValue(PREF_LAST_DIM_STANDARD, dimensionOptions[1].selected.toString())
        props.setValue(PREF_LAST_DIM_SECURITY, dimensionOptions[2].selected.toString())
        props.setValue(PREF_LAST_CHECKER, selectedChecker ?: "")
        logger.info("云端代码检查配置已保存")
    }

    /**
     * 从持久化存储恢复筛选配置（不含任务选中，任务在加载列表后恢复）
     */
    private fun restoreConfigUI() {
        val props = PropertiesComponent.getInstance(project)
        dimensionOptions[0].selected = props.getBoolean(PREF_LAST_DIM_DEFECT, true)
        dimensionOptions[1].selected = props.getBoolean(PREF_LAST_DIM_STANDARD, true)
        dimensionOptions[2].selected = props.getBoolean(PREF_LAST_DIM_SECURITY, true)
        updateDimensionComboBoxText()
        val savedChecker = props.getValue(PREF_LAST_CHECKER, "")
        if (savedChecker.isNotBlank()) {
            selectedChecker = savedChecker
            isUpdatingComboBox = true
            checkerComboBox.removeAllItems()
            checkerComboBox.addItem(savedChecker)
            checkerComboBox.selectedIndex = 0
            isUpdatingComboBox = false
        }
    }

    /**
     * 获取上次保存的任务 ID
     */
    private fun getSavedTaskId(): Long? {
        val value = PropertiesComponent.getInstance(project).getValue(PREF_LAST_TASK_ID)
        return value?.toLongOrNull()
    }

    /**
     * 当"云端代码检查"tab 被激活时调用
     *
     * 首次激活时：恢复历史配置，加载任务列表，自动执行查询。
     */
    fun onTabActivated() {
        if (hasAutoQueried) return
        hasAutoQueried = true

        val savedTaskId = getSavedTaskId() ?: return
        restoreConfigUI()

        logger.info("云端代码检查 tab 激活，恢复历史配置，任务ID: $savedTaskId")

        coroutineScope.launch {
            try {
                val codeccService = CodeCCService.getInstance(project)
                val result = codeccService.getRemoteTaskList()

                SwingUtilities.invokeLater {
                    isLoadingTasks = false
                    when (result) {
                        is RemoteTaskListResult.Success -> {
                            lastTaskLoadFailureTime = 0
                            updateTaskComboBox(result.tasks, selectTaskId = savedTaskId)

                            if (selectedTask != null && selectedTask!!.taskId == savedTaskId) {
                                handleQueryDefects()
                            }
                        }
                        is RemoteTaskListResult.Failure -> {
                            lastTaskLoadFailureTime = System.currentTimeMillis()
                            logger.warn("自动加载任务列表失败: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("自动加载任务列表异常: ${e.message}", e)
                SwingUtilities.invokeLater {
                    isLoadingTasks = false
                    lastTaskLoadFailureTime = System.currentTimeMillis()
                }
            }
        }
    }

    /** 处理清除结果 */
    private fun handleClear() {
        logger.info("用户点击清除结果")
        clear()
    }

    /** 处理筛选条件变化 */
    private fun handleFilterChange() {
        showCritical = criticalFilterButton.isSelected
        showNormal = normalFilterButton.isSelected
        showInfo = infoFilterButton.isSelected
        logger.info("筛选条件变化: 严重=$showCritical, 一般=$showNormal, 提示=$showInfo")
        rebuildTree()
    }

    // ========== 树形展示逻辑 ==========

    /**
     * 显示缺陷列表（按文件名分组）
     *
     * 复用与本地代码检查相同的展示风格。
     *
     * @param defects 缺陷列表（已转换为本地 Defect 格式）
     */
    private fun showDefects(defects: List<Defect>) {
        rootNode.removeAllChildren()
        currentDefects = defects

        if (defects.isEmpty()) {
            rootNode.add(DefaultMutableTreeNode("\u2713 未发现代码问题"))
            treeModel.reload()
            return
        }

        val filteredDefects = defects.filter { defect ->
            val severityMatch = when (defect.severity) {
                1L -> showCritical
                2L -> showNormal
                4L -> showInfo
                else -> showInfo
            }
            val checkerMatch = selectedChecker?.let {
                "${defect.toolName}#${defect.checkerName}" == it
            } ?: true
            severityMatch && checkerMatch
        }

        if (filteredDefects.isEmpty()) {
            rootNode.add(DefaultMutableTreeNode("无匹配的告警（已被筛选器过滤）"))
            treeModel.reload()
            return
        }

        val groupedByFile = filteredDefects.groupBy { it.filePath }

        groupedByFile.forEach { (filePath, fileDefects) ->
            val fileName = filePath.substringAfterLast('/').substringAfterLast('\\')
            val issueCount = fileDefects.size
            val fileNode = DefaultMutableTreeNode(FileNodeData(filePath, fileName, issueCount))

            fileDefects.forEach { defect ->
                fileNode.add(DefaultMutableTreeNode(DefectNodeData(defect)))
            }

            rootNode.add(fileNode)
        }

        treeModel.reload()
        expandAllNodes()
    }

    /** 重建树结构 */
    private fun rebuildTree() {
        showDefects(currentDefects)
    }

    /** 展开所有节点 */
    private fun expandAllNodes() {
        for (i in 0 until defectTree.rowCount) {
            defectTree.expandRow(i)
        }
    }

    /** 选中告警节点时自动跳转到对应代码位置（与本地代码检查行为一致） */
    @Suppress("TooGenericExceptionCaught")
    private fun navigateToSelectedDefect() {
        val path = defectTree.selectionPath ?: return
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
        val userObject = node.userObject

        if (userObject !is DefectNodeData) return

        try {
            val defect = userObject.defect
            if (defect.filePath.isEmpty()) {
                logger.warn("文件路径为空，无法跳转")
                return
            }

            val lineNumber = if (defect.line >= 1) defect.line - 1 else 0
            val normalizedPath = PathHelper.toIntelliJPath(defect.filePath)
            logger.info("跳转到代码位置: $normalizedPath:${lineNumber + 1}")

            val virtualFile = LocalFileSystem.getInstance().findFileByPath(normalizedPath)
            if (virtualFile == null || !virtualFile.exists()) {
                logger.warn("无法找到文件: ${defect.filePath}")
                Messages.showWarningDialog(
                    project,
                    "该告警位于 ${defect.filePath}#${defect.line}，无法定位，请确认本地项目跟云端项目是否对齐。",
                    "无法定位文件"
                )
                return
            }

            OpenFileDescriptor(project, virtualFile, lineNumber, 0).navigate(true)
        } catch (e: Exception) {
            logger.error("跳转到代码位置失败: ${e.message}", e)
        }
    }

    // ========== 辅助方法 ==========

    /** 显示消息 */
    private fun showMessage(message: String) {
        rootNode.removeAllChildren()
        rootNode.add(DefaultMutableTreeNode(message))
        treeModel.reload()
    }

    /** 显示错误消息 */
    private fun showErrorMessage(message: String) {
        rootNode.removeAllChildren()
        rootNode.add(DefaultMutableTreeNode("\u2717 $message"))
        treeModel.reload()
        summaryLabel.text = " "
    }

    /** 清除所有结果 */
    fun clear() {
        rootNode.removeAllChildren()
        currentDefects = emptyList()
        treeModel.reload()
        summaryLabel.text = " "
    }

    /** 刷新（重新获取任务列表） */
    fun refresh() {
        handleRefreshTasks()
    }

    /**
     * 获取主面板组件
     *
     * @return 主面板
     */
    fun getContent(): JComponent {
        return mainPanel
    }

    // ========== 内部数据类 ==========

    /** 文件节点数据 */
    private data class FileNodeData(
        val filePath: String,
        val fileName: String,
        val issueCount: Int
    ) {
        override fun toString(): String {
            val issueText = if (issueCount == 1) "issue" else "issues"
            return "$fileName: $issueCount $issueText"
        }
    }

    /** 缺陷节点数据 */
    private data class DefectNodeData(val defect: Defect) {
        override fun toString(): String {
            return "${defect.description} (${defect.line}) [${defect.toolName}#${defect.checkerName}]"
        }
    }

    /** 任务下拉框渲染器 */
    private class TaskComboBoxRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): java.awt.Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is RemoteTaskInfo) {
                text = value.getDisplayName()
            }
            return this
        }
    }

    /** 自定义树节点渲染器 */
    private inner class RemoteDefectTreeCellRenderer : DefaultTreeCellRenderer() {
        override fun getTreeCellRendererComponent(
            tree: JTree?,
            value: Any?,
            sel: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): java.awt.Component {
            val component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)

            if (value is DefaultMutableTreeNode) {
                when (val userObject = value.userObject) {
                    is FileNodeData -> {
                        icon = AllIcons.FileTypes.Any_type
                    }
                    is DefectNodeData -> {
                        icon = when (userObject.defect.severity) {
                            1L -> GroupedScanResultPanel.Icons.ERROR
                            2L -> GroupedScanResultPanel.Icons.WARNING
                            4L -> GroupedScanResultPanel.Icons.NORMAL
                            else -> GroupedScanResultPanel.Icons.NORMAL
                        }
                    }
                }
            }

            return component
        }
    }
}
