package com.codecc.preci.ui.toolwindow

import com.codecc.preci.api.model.response.Defect
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.scan.ScanResultQueryResult
import com.codecc.preci.util.PathHelper
import com.codecc.preci.service.scan.ScanService
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.RowFilter
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

/**
 * 扫描结果展示面板
 *
 * 提供代码扫描结果的可视化展示，包括：
 * - 缺陷列表（表格形式）
 * - 结果过滤（按工具、规则、文件类型）- 可选
 * - 点击跳转到代码位置
 * - 结果刷新
 *
 * **功能特性：**
 * - 表格展示：使用 JBTable 展示缺陷信息（严重程度、工具、规则、描述、文件、行号）
 * - 实时过滤：支持按工具名、规则名、文件路径过滤（如果启用）
 * - 双击跳转：双击表格行可跳转到对应代码位置
 * - 异步加载：使用协程异步加载扫描结果，不阻塞 UI 线程
 *
 * **布局结构：**
 * ```
 * ┌────────────────────────────────────────┐
 * │  过滤栏（工具、规则、文件路径）(可选)   │
 * ├────────────────────────────────────────┤
 * │                                        │
 * │  缺陷列表表格                           │
 * │  (严重程度 | 工具 | 规则 | 描述 | 文件 | 行号) │
 * │                                        │
 * └────────────────────────────────────────┘
 * ```
 *
 * @property project 当前项目
 * @property coroutineScope 协程作用域，用于异步操作
 * @property showFilters 是否显示筛选栏，默认为 true
 * @since 1.0
 */
class ScanResultPanel(
    private val project: Project,
    private val coroutineScope: CoroutineScope,
    private val showFilters: Boolean = true
) {

    private val logger = PreCILogger.getLogger(ScanResultPanel::class.java)

    /**
     * 主面板
     */
    private val mainPanel: JPanel = JBPanel<JBPanel<*>>(BorderLayout())

    /**
     * 表格模型
     */
    private val tableModel: DefaultTableModel = object : DefaultTableModel(
        arrayOf("严重程度", "工具", "规则", "描述", "文件", "行号"),
        0
    ) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }

    /**
     * 缺陷列表表格
     */
    private val defectsTable: JBTable = JBTable(tableModel).apply {
        // 设置表格属性
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        autoCreateRowSorter = true // 启用列排序
        fillsViewportHeight = true

        // 设置列宽
        columnModel.getColumn(0).preferredWidth = 80   // 严重程度
        columnModel.getColumn(1).preferredWidth = 120  // 工具
        columnModel.getColumn(2).preferredWidth = 120  // 规则
        columnModel.getColumn(3).preferredWidth = 280  // 描述
        columnModel.getColumn(4).preferredWidth = 280  // 文件
        columnModel.getColumn(5).preferredWidth = 60   // 行号

        // 添加双击监听器，实现跳转功能
        addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    handleDoubleClick()
                }
            }
        })
    }

    /**
     * 表格排序器（用于过滤）
     */
    private val rowSorter: TableRowSorter<DefaultTableModel> = TableRowSorter(tableModel).apply {
        defectsTable.rowSorter = this
    }

    /**
     * 工具名过滤输入框
     */
    private val toolFilterField: JTextField = JTextField(15).apply {
        toolTipText = "按工具名过滤（支持正则表达式）"
    }

    /**
     * 规则名过滤输入框
     */
    private val checkerFilterField: JTextField = JTextField(15).apply {
        toolTipText = "按规则名过滤（支持正则表达式）"
    }

    /**
     * 文件路径过滤输入框
     */
    private val fileFilterField: JTextField = JTextField(20).apply {
        toolTipText = "按文件路径过滤（支持正则表达式）"
    }

    /**
     * 当前显示的缺陷列表（未过滤）
     */
    private var currentDefects: List<Defect> = emptyList()

    init {
        initializeLayout()
        setupFilterListeners()
    }

    /**
     * 初始化面板布局
     */
    private fun initializeLayout() {
        // 如果启用筛选功能，创建过滤栏
        if (showFilters) {
            val filterPanel = createFilterPanel()
            mainPanel.add(filterPanel, BorderLayout.NORTH)
        }

        // 创建表格滚动面板
        val scrollPane = JBScrollPane(defectsTable)
        scrollPane.border = JBUI.Borders.empty()
        mainPanel.add(scrollPane, BorderLayout.CENTER)
    }

    /**
     * 创建过滤栏面板
     *
     * @return 过滤栏面板
     */
    private fun createFilterPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>()
        panel.layout = GridBagLayout()
        panel.border = JBUI.Borders.empty(5)

        val gbc = GridBagConstraints()
        gbc.insets = JBUI.insets(2, 5)
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST

        // 工具过滤
        gbc.gridx = 0
        panel.add(JBLabel("工具:"), gbc)
        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 0.3
        panel.add(toolFilterField, gbc)

        // 规则过滤
        gbc.gridx = 2
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel("规则:"), gbc)
        gbc.gridx = 3
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 0.3
        panel.add(checkerFilterField, gbc)

        // 文件过滤
        gbc.gridx = 4
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel("文件:"), gbc)
        gbc.gridx = 5
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 0.4
        panel.add(fileFilterField, gbc)

        return panel
    }

    /**
     * 设置过滤器监听器
     *
     * 监听过滤输入框的变化，实时更新表格显示
     */
    private fun setupFilterListeners() {
        val filterAction = {
            applyFilters()
        }

        // 为每个过滤输入框添加文档监听器
        toolFilterField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = filterAction()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = filterAction()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = filterAction()
        })

        checkerFilterField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = filterAction()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = filterAction()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = filterAction()
        })

        fileFilterField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = filterAction()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = filterAction()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = filterAction()
        })
    }

    /**
     * 应用过滤器
     *
     * 根据过滤输入框的内容，过滤表格中的数据
     */
    @Suppress("TooGenericExceptionCaught") // 需要捕获所有异常以保证 UI 稳定性
    private fun applyFilters() {
        try {
            val toolFilter = toolFilterField.text.trim()
            val checkerFilter = checkerFilterField.text.trim()
            val fileFilter = fileFilterField.text.trim()

            // 如果所有过滤条件都为空，清除过滤器
            if (toolFilter.isEmpty() && checkerFilter.isEmpty() && fileFilter.isEmpty()) {
                rowSorter.rowFilter = null
                return
            }

            // 创建组合过滤器
            val filters = mutableListOf<RowFilter<DefaultTableModel, Int>>()

            if (toolFilter.isNotEmpty()) {
                filters.add(RowFilter.regexFilter("(?i)$toolFilter", 1)) // 工具列（列索引 1）
            }
            if (checkerFilter.isNotEmpty()) {
                filters.add(RowFilter.regexFilter("(?i)$checkerFilter", 2)) // 规则列（列索引 2）
            }
            if (fileFilter.isNotEmpty()) {
                filters.add(RowFilter.regexFilter("(?i)$fileFilter", 4)) // 文件列（列索引 4）
            }

            // 应用 AND 组合过滤器
            rowSorter.rowFilter = RowFilter.andFilter(filters)

        } catch (e: Exception) {
            logger.warn("应用过滤器失败: ${e.message}", e)
            // 过滤失败时不中断，只记录日志
        }
    }

    /**
     * 处理表格双击事件
     *
     * 双击表格行时，跳转到对应的代码位置
     */
    @Suppress("TooGenericExceptionCaught") // 需要捕获所有异常以保证 UI 稳定性
    private fun handleDoubleClick() {
        val selectedRow = defectsTable.selectedRow
        if (selectedRow < 0) {
            return
        }

        try {
            // 获取表格中的数据（注意：需要转换为模型索引）
            val modelRow = defectsTable.convertRowIndexToModel(selectedRow)
            
            // 检查是否是数据行（排除加载中、无缺陷等提示行）
            val severityText = tableModel.getValueAt(modelRow, 0) as? String
            if (severityText == null || severityText.isEmpty() || 
                severityText == "加载中..." || severityText == "✓ 未发现代码问题") {
                return
            }
            
            val filePath = tableModel.getValueAt(modelRow, 4) as? String
            val lineValue = tableModel.getValueAt(modelRow, 5)
            
            if (filePath.isNullOrEmpty()) {
                logger.warn("文件路径为空，无法跳转")
                return
            }
            
            // 处理行号：API 返回的行号是从 1 开始的，需要转换为从 0 开始
            val lineNumber = when (lineValue) {
                is Int -> {
                    if (lineValue < 1) {
                        logger.warn("无效的行号: $lineValue，行号应该 >= 1")
                        return
                    }
                    lineValue - 1  // 转换为从 0 开始的索引
                }
                is String -> {
                    try {
                        val parsed = lineValue.toInt()
                        if (parsed < 1) {
                            logger.warn("无效的行号: $parsed，行号应该 >= 1")
                            return
                        }
                        parsed - 1
                    } catch (e: NumberFormatException) {
                        logger.warn("无法解析行号: $lineValue", e)
                        return
                    }
                }
                else -> {
                    logger.warn("未知的行号类型: ${lineValue?.javaClass?.name}")
                    return
                }
            }

            val normalizedPath = PathHelper.toIntelliJPath(filePath)
            logger.info("跳转到代码位置: $normalizedPath:${lineNumber + 1} (索引: $lineNumber)")

            // 查找虚拟文件
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(normalizedPath)
            if (virtualFile == null) {
                logger.warn("无法找到文件: $filePath")
                return
            }
            
            if (!virtualFile.exists()) {
                logger.warn("文件不存在: $filePath")
                return
            }

            // 跳转到文件和行号
            // OpenFileDescriptor 的行号参数是从 0 开始的
            // 如果行号超出文件范围，会自动调整到文件末尾
            // navigate(true) 表示请求焦点并跳转
            val descriptor = OpenFileDescriptor(project, virtualFile, lineNumber, 0)
            descriptor.navigate(true)
            
            logger.info("成功跳转到文件: $filePath，行号: ${lineNumber + 1}")

        } catch (e: ClassCastException) {
            logger.error("类型转换失败，表格数据格式可能不正确: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("跳转到代码位置失败: ${e.message}", e)
        }
    }

    /**
     * 加载扫描结果
     *
     * 从 ScanService 异步加载扫描结果，并更新表格显示
     *
     * @param path 查询路径（可选，默认为项目根目录）
     */
    @Suppress("TooGenericExceptionCaught") // 需要捕获所有异常以保证健壮性
    fun loadScanResult(path: String? = null) {
        coroutineScope.launch {
            try {
                logger.info("开始加载扫描结果")
                
                // 显示加载状态（在 Swing EDT 线程中）
                SwingUtilities.invokeLater {
                    clearTable()
                    showLoadingMessage()
                }

                // 调用 ScanService 获取结果
                val scanService = ScanService.getInstance(project)
                val result = scanService.getScanResult(path)

                // 更新 UI（在 Swing EDT 线程中）
                SwingUtilities.invokeLater {
                    when (result) {
                        is ScanResultQueryResult.Success -> {
                            val defects = result.response.getDefectList()
                            showDefects(defects)
                            logger.info("加载扫描结果成功: ${defects.size} 个缺陷")
                        }
                        is ScanResultQueryResult.Failure -> {
                            showErrorMessage("加载失败: ${result.message}")
                            logger.error("加载扫描结果失败: ${result.message}", result.exception)
                        }
                    }
                }

            } catch (e: Exception) {
                logger.error("加载扫描结果异常: ${e.message}", e)
                SwingUtilities.invokeLater {
                    showErrorMessage("加载失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 显示缺陷列表
     *
     * @param defects 缺陷列表
     */
    private fun showDefects(defects: List<Defect>) {
        clearTable()
        currentDefects = defects

        if (defects.isEmpty()) {
            showNoDefectsMessage()
            return
        }

        // 添加数据到表格
        defects.forEach { defect ->
            tableModel.addRow(
                arrayOf(
                    defect.getSeverityText(),
                    defect.toolName,
                    defect.checkerName,
                    defect.description,
                    defect.filePath,
                    defect.line
                )
            )
        }
    }

    /**
     * 显示加载消息
     */
    private fun showLoadingMessage() {
        tableModel.addRow(
            arrayOf("加载中...", "", "", "", "", "")
        )
    }

    /**
     * 显示无缺陷消息
     */
    private fun showNoDefectsMessage() {
        tableModel.addRow(
            arrayOf("✓ 未发现代码问题", "", "", "", "", "")
        )
    }

    /**
     * 显示错误消息
     *
     * @param message 错误消息
     */
    private fun showErrorMessage(message: String) {
        tableModel.addRow(
            arrayOf("✗ $message", "", "", "", "", "")
        )
    }

    /**
     * 清空表格
     */
    fun clearTable() {
        tableModel.rowCount = 0
        currentDefects = emptyList()
    }

    /**
     * 刷新结果
     *
     * 重新加载扫描结果
     */
    fun refresh() {
        loadScanResult()
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
     * 获取当前缺陷数量
     *
     * @return 缺陷数量
     */
    fun getDefectCount(): Int {
        return currentDefects.size
    }

    /**
     * 获取当前显示的缺陷数量（过滤后）
     *
     * @return 显示的缺陷数量
     */
    fun getVisibleDefectCount(): Int {
        return defectsTable.rowCount
    }
}

