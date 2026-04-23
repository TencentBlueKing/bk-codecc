package com.codecc.preci.ui.toolwindow

import com.codecc.preci.api.model.response.Defect
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.scan.ScanResultQueryResult
import com.codecc.preci.util.PathHelper
import com.codecc.preci.service.scan.ScanService
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

/**
 * 分组扫描结果展示面板（按文件名折叠式）
 *
 * 提供按文件名分组的折叠式扫描结果展示，包括：
 * - 按文件名分组的树形结构
 * - 每个文件显示告警数量
 * - 支持严重程度筛选
 * - 点击跳转到代码位置
 *
 * **布局结构：**
 * ```
 * ┌────────────────────────────────────────┐
 * │ ▼ GetTosaChartAction.java: 3 issues    │
 * │   ├─ 🔴 第 1 个字符 '{' 应位于前一行   │
 * │   ├─ 🟡 第 5 个字符...                 │
 * │   └─ 🔵 本行字符数 154个...            │
 * │ ▶ CodeccExtSdkApi.kt: 568 issues       │
 * └────────────────────────────────────────┘
 * ```
 *
 * @property project 当前项目
 * @property coroutineScope 协程作用域，用于异步操作
 * @since 1.0
 */
class GroupedScanResultPanel(
    private val project: Project,
    private val coroutineScope: CoroutineScope
) {

    private val logger = PreCILogger.getLogger(GroupedScanResultPanel::class.java)

    /**
     * 主面板
     */
    private val mainPanel: JPanel = JBPanel<JBPanel<*>>(BorderLayout())

    /**
     * 树形根节点
     */
    private val rootNode = DefaultMutableTreeNode("扫描结果")

    /**
     * 树模型
     */
    private val treeModel = DefaultTreeModel(rootNode)

    /**
     * 树形组件
     */
    private val defectTree: Tree = Tree(treeModel).apply {
        isRootVisible = false
        showsRootHandles = true

        // 设置行高
        rowHeight = 24

        // 自定义渲染器
        cellRenderer = DefectTreeCellRenderer()

        addTreeSelectionListener { handleDoubleClick() }
    }

    /**
     * 筛选器状态
     */
    private var showCritical = true
    private var showNormal = true
    private var showInfo = true

    /**
     * 当前显示的缺陷列表（未过滤）
     */
    private var currentDefects: List<Defect> = emptyList()

    /**
     * 图标加载器
     *
     * 使用 lazy 初始化以兼容测试环境（测试环境中 JBUIScale 未预计算，
     * 直接调用 IconLoader 会导致 ExceptionInInitializerError）。
     */
    object Icons {
        val ERROR: Icon by lazy { safeLoadIcon("/icon/error.svg") }
        val WARNING: Icon by lazy { safeLoadIcon("/icon/warning.svg") }
        val NORMAL: Icon by lazy { safeLoadIcon("/icon/normal.svg") }

        private fun safeLoadIcon(path: String): Icon {
            return try {
                IconLoader.getIcon(path, Icons::class.java)
            } catch (_: Throwable) {
                UIManager.getIcon("OptionPane.errorIcon") ?: object : Icon {
                    override fun paintIcon(c: java.awt.Component?, g: java.awt.Graphics?, x: Int, y: Int) {}
                    override fun getIconWidth(): Int = 16
                    override fun getIconHeight(): Int = 16
                }
            }
        }
    }

    init {
        initializeLayout()
    }

    /**
     * 初始化面板布局
     */
    private fun initializeLayout() {
        // 创建树形滚动面板
        val scrollPane = JBScrollPane(defectTree)
        scrollPane.border = JBUI.Borders.empty()
        mainPanel.add(scrollPane, BorderLayout.CENTER)
    }

    /**
     * 设置严重告警筛选状态
     */
    fun setShowCritical(show: Boolean) {
        showCritical = show
        rebuildTree()
    }

    /**
     * 设置一般告警筛选状态
     */
    fun setShowNormal(show: Boolean) {
        showNormal = show
        rebuildTree()
    }

    /**
     * 设置提示告警筛选状态
     */
    fun setShowInfo(show: Boolean) {
        showInfo = show
        rebuildTree()
    }

    /**
     * 获取严重告警筛选状态
     */
    fun isShowCritical(): Boolean = showCritical

    /**
     * 获取一般告警筛选状态
     */
    fun isShowNormal(): Boolean = showNormal

    /**
     * 获取提示告警筛选状态
     */
    fun isShowInfo(): Boolean = showInfo

    /**
     * 重建树结构
     */
    private fun rebuildTree() {
        showDefects(currentDefects)
    }

    /**
     * 展开所有节点
     */
    private fun expandAllNodes() {
        for (i in 0 until defectTree.rowCount) {
            defectTree.expandRow(i)
        }
    }

    /**
     * 处理树节点双击事件
     */
    @Suppress("TooGenericExceptionCaught")
    private fun handleDoubleClick() {
        val path = defectTree.selectionPath ?: return
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
        val userObject = node.userObject

        // 只处理缺陷节点（不是文件分组节点）
        if (userObject !is DefectNodeData) {
            return
        }

        try {
            val defect = userObject.defect
            val filePath = defect.filePath

            if (filePath.isEmpty()) {
                logger.warn("文件路径为空，无法跳转")
                return
            }

            // 处理行号
            val lineNumber = if (defect.line >= 1) defect.line - 1 else 0

            val normalizedPath = PathHelper.toIntelliJPath(filePath)
            logger.info("跳转到代码位置: $normalizedPath:${lineNumber + 1}")

            val virtualFile = LocalFileSystem.getInstance().findFileByPath(normalizedPath)
            if (virtualFile == null || !virtualFile.exists()) {
                logger.warn("无法找到文件: $filePath")
                return
            }

            val descriptor = OpenFileDescriptor(project, virtualFile, lineNumber, 0)
            descriptor.navigate(true)

            logger.info("成功跳转到文件: $filePath，行号: ${lineNumber + 1}")

        } catch (e: Exception) {
            logger.error("跳转到代码位置失败: ${e.message}", e)
        }
    }

    /**
     * 加载扫描结果
     */
    @Suppress("TooGenericExceptionCaught")
    fun loadScanResult(path: String? = null) {
        coroutineScope.launch {
            try {
                logger.info("开始加载扫描结果")

                SwingUtilities.invokeLater {
                    clearTree()
                    showLoadingMessage()
                }

                val scanService = ScanService.getInstance(project)
                val result = scanService.getScanResult(path)

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
     * 显示缺陷列表（按文件名分组）
     */
    private fun showDefects(defects: List<Defect>) {
        rootNode.removeAllChildren()
        currentDefects = defects

        if (defects.isEmpty()) {
            showNoDefectsMessage()
            return
        }

        // 根据筛选条件过滤缺陷
        val filteredDefects = defects.filter { defect ->
            when (defect.severity) {
                1L -> showCritical
                2L -> showNormal
                4L -> showInfo
                else -> showInfo
            }
        }

        if (filteredDefects.isEmpty()) {
            val node = DefaultMutableTreeNode("无匹配的告警（已被筛选器过滤）")
            rootNode.add(node)
            treeModel.reload()
            return
        }

        // 按文件名分组
        val groupedByFile = filteredDefects.groupBy { it.filePath }

        // 为每个文件创建分组节点
        groupedByFile.forEach { (filePath, fileDefects) ->
            val fileName = filePath.substringAfterLast('/').substringAfterLast('\\')
            val issueCount = fileDefects.size

            val fileNode = DefaultMutableTreeNode(FileNodeData(filePath, fileName, issueCount))

            // 添加缺陷到文件分组
            fileDefects.forEach { defect ->
                val defectNode = DefaultMutableTreeNode(DefectNodeData(defect))
                fileNode.add(defectNode)
            }

            rootNode.add(fileNode)
        }

        treeModel.reload()
        expandAllNodes()
    }

    /**
     * 显示加载消息
     */
    private fun showLoadingMessage() {
        rootNode.removeAllChildren()
        rootNode.add(DefaultMutableTreeNode("加载中..."))
        treeModel.reload()
    }

    /**
     * 显示无缺陷消息
     */
    private fun showNoDefectsMessage() {
        rootNode.removeAllChildren()
        rootNode.add(DefaultMutableTreeNode("✓ 未发现代码问题"))
        treeModel.reload()
    }

    /**
     * 显示错误消息
     */
    private fun showErrorMessage(message: String) {
        rootNode.removeAllChildren()
        rootNode.add(DefaultMutableTreeNode("✗ $message"))
        treeModel.reload()
    }

    /**
     * 清空树
     */
    private fun clearTree() {
        rootNode.removeAllChildren()
        currentDefects = emptyList()
    }

    /**
     * 清除所有结果
     */
    fun clear() {
        clearTree()
        treeModel.reload()
    }

    /**
     * 刷新结果
     */
    fun refresh() {
        loadScanResult()
    }

    /**
     * 获取主面板组件
     */
    fun getContent(): JComponent {
        return mainPanel
    }

    /**
     * 获取当前缺陷数量
     */
    fun getDefectCount(): Int {
        return currentDefects.size
    }

    /**
     * 文件节点数据
     */
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

    /**
     * 缺陷节点数据
     */
    private data class DefectNodeData(val defect: Defect) {
        override fun toString(): String {
            // 格式：描述 (行号) [工具名#规则名]
            return "${defect.description} (${defect.line}) [${defect.toolName}#${defect.checkerName}]"
        }
    }

    /**
     * 自定义树节点渲染器
     */
    private inner class DefectTreeCellRenderer : DefaultTreeCellRenderer() {
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
                val userObject = value.userObject

                when (userObject) {
                    is FileNodeData -> {
                        // 文件节点使用文件图标
                        icon = AllIcons.FileTypes.Any_type
                    }
                    is DefectNodeData -> {
                        // 根据严重程度设置图标
                        icon = when (userObject.defect.severity) {
                            1L -> Icons.ERROR
                            2L -> Icons.WARNING
                            4L -> Icons.NORMAL
                            else -> Icons.NORMAL
                        }
                    }
                }
            }

            return component
        }
    }
}
