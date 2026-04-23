package com.codecc.preci.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * PreCI 工具窗口主面板
 *
 * 提供 PreCI 插件的主要 UI 界面，采用简洁设计。
 *
 * **功能概述：**
 * - 项目初始化（集成在选项卡标题栏中）
 * - 本地代码检查（包含当前文件、变更文件、所有文件三个子选项卡）
 * - 云端代码检查（CodeCC 平台远程缺陷查看）
 *
 * **布局结构：**
 * ```
 * ┌──────────────────────────────────────────────────────────┐
 * │  本地代码检查  云端代码检查 │ ⚙ 初始化                    │ ← 自定义标题栏
 * │  ───────────                                             │   (蓝色下划线仅在选中 tab 下方)
 * ├──────────────────────────────────────────────────────────┤
 * │                                                          │
 * │  内容区域 (CardLayout 切换)                               │
 * │                                                          │
 * └──────────────────────────────────────────────────────────┘
 * ```
 *
 * 使用自定义 Tab Header + CardLayout 替代 JBTabbedPane，
 * 以确保 tab 选中下划线不会延伸到"初始化"按钮。
 *
 * @property project 当前项目实例
 * @since 1.0
 */
class PreCIToolWindow(private val project: Project) {

    /**
     * 协程作用域
     *
     * 用于管理所有异步操作，使用 SupervisorJob 防止单个任务失败影响其他任务
     */
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 主面板容器 */
    private val mainPanel: JPanel = JBPanel<JBPanel<*>>(BorderLayout())

    /** 初始化项目面板 */
    private val initProjectPanel: InitProjectPanel = InitProjectPanel(project, coroutineScope)

    /** 本地代码检查面板 */
    private val localCodeCheckPanel: LocalCodeCheckPanel = LocalCodeCheckPanel(project, coroutineScope)

    /** 云端代码检查面板 */
    private val cloudCodeCheckPanel: CloudCodeCheckPanel = CloudCodeCheckPanel(project, coroutineScope)

    /** PAC 流水线面板 */
    private val pacPipelinePanel: PacPipelinePanel = PacPipelinePanel(project, coroutineScope)

    /** 内容面板，使用 CardLayout 切换不同 tab 的内容 */
    private val contentPanel: JPanel = JPanel(CardLayout())

    /** 各 tab 的标签文本组件 */
    private val tabLabels = mutableListOf<JLabel>()

    /** 各 tab 的选中下划线组件 */
    private val tabUnderlines = mutableListOf<JPanel>()

    /** 当前选中的 tab 索引 */
    private var selectedTabIndex = 0

    init {
        initializeLayout()
    }

    /**
     * 初始化主面板布局
     *
     * 标题栏：本地代码检查 | 云端代码检查 | [分隔线] ⚙ 初始化
     * 内容区：CardLayout 切换两个面板
     */
    private fun initializeLayout() {
        mainPanel.border = JBUI.Borders.empty(0)

        contentPanel.add(localCodeCheckPanel.getContent(), TAB_KEY_LOCAL)
        contentPanel.add(cloudCodeCheckPanel.getContent(), TAB_KEY_CLOUD)
        contentPanel.add(pacPipelinePanel.getContent(), TAB_KEY_PIPELINE)

        val headerPanel = createHeaderPanel()

        mainPanel.add(headerPanel, BorderLayout.NORTH)
        mainPanel.add(contentPanel, BorderLayout.CENTER)

        selectTab(0)
    }

    /**
     * 创建自定义标题栏
     *
     * 布局：[本地代码检查] [云端代码检查] | [分隔线] | ⚙ 初始化
     * 每个 tab 是一个独立的可点击组件，选中时底部显示蓝色下划线。
     */
    private fun createHeaderPanel(): JPanel {
        val header = JBPanel<JBPanel<*>>()
        header.layout = BoxLayout(header, BoxLayout.X_AXIS)
        header.isOpaque = false
        header.border = JBUI.Borders.customLineBottom(JBColor.border())

        header.add(createTabButton("本地代码检查", 0))
        header.add(createTabButton("云端代码检查", 1))
        header.add(createTabButton("PAC流水线", 2))

        header.add(Box.createHorizontalStrut(JBUI.scale(15)))

        val separator = JSeparator(SwingConstants.VERTICAL)
        separator.maximumSize = Dimension(JBUI.scale(1), JBUI.scale(20))
        header.add(separator)

        header.add(Box.createHorizontalStrut(JBUI.scale(10)))

        val initContent = initProjectPanel.getContent()
        initContent.maximumSize = initContent.preferredSize
        header.add(initContent)

        header.add(Box.createHorizontalGlue())

        return header
    }

    /**
     * 创建单个 tab 按钮组件
     *
     * 由标签文本和底部下划线组成，点击时切换到对应 tab。
     *
     * @param text Tab 显示文本
     * @param index Tab 索引
     * @return Tab 按钮面板
     */
    private fun createTabButton(text: String, index: Int): JPanel {
        val tabPanel = JBPanel<JBPanel<*>>(BorderLayout())
        tabPanel.isOpaque = false

        val label = JLabel(text)
        label.border = JBUI.Borders.empty(4, 8)
        label.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        tabLabels.add(label)

        val underline = JPanel()
        underline.preferredSize = Dimension(0, JBUI.scale(2))
        underline.isOpaque = false
        tabUnderlines.add(underline)

        tabPanel.add(label, BorderLayout.CENTER)
        tabPanel.add(underline, BorderLayout.SOUTH)

        // 限制最大尺寸，防止 BoxLayout 将 tab 拉伸填满整行
        tabPanel.maximumSize = tabPanel.preferredSize

        label.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                selectTab(index)
            }
        })

        return tabPanel
    }

    /**
     * 切换到指定 tab
     *
     * 更新内容区显示、tab 文字颜色和下划线状态。
     *
     * @param index 目标 tab 索引
     */
    private fun selectTab(index: Int) {
        selectedTabIndex = index

        val cardLayout = contentPanel.layout as CardLayout
        cardLayout.show(contentPanel, TAB_KEYS[index])

        tabLabels.forEachIndexed { i, label ->
            label.foreground = if (i == index) {
                JBColor.foreground()
            } else {
                JBColor.GRAY
            }
        }

        tabUnderlines.forEachIndexed { i, underline ->
            if (i == index) {
                underline.isOpaque = true
                underline.background = TAB_UNDERLINE_COLOR
            } else {
                underline.isOpaque = false
                underline.background = null
            }
            underline.repaint()
        }

        if (index == TAB_INDEX_CLOUD) {
            cloudCodeCheckPanel.onTabActivated()
        }
        if (index == TAB_INDEX_PIPELINE) {
            pacPipelinePanel.onTabActivated()
        }
    }

    companion object {
        private const val TAB_KEY_LOCAL = "local"
        private const val TAB_KEY_CLOUD = "cloud"
        private const val TAB_KEY_PIPELINE = "pipeline"
        private val TAB_KEYS = listOf(TAB_KEY_LOCAL, TAB_KEY_CLOUD, TAB_KEY_PIPELINE)
        private const val TAB_INDEX_CLOUD = 1
        private const val TAB_INDEX_PIPELINE = 2

        /** Tab 选中时的下划线颜色（蓝色，兼容 Light/Dark 主题） */
        private val TAB_UNDERLINE_COLOR = JBColor(0x4083C9, 0x4A88C7)
    }

    /**
     * 获取主面板组件
     *
     * @return 工具窗口的根 JComponent
     */
    fun getContent(): JComponent {
        return mainPanel
    }

    /** 刷新内容 */
    fun refresh() {
        localCodeCheckPanel.refresh()
    }

    /**
     * 切换到本地检查的扫描进度视图并启动轮询
     *
     * 供外部 Action 在扫描成功启动后调用，确保先切换到本地代码检查 Tab，
     * 再展示进度面板。
     */
    fun showScanProgress() {
        selectTab(0)
        localCodeCheckPanel.showScanProgress()
    }

    /** 清空内容 */
    fun clear() {
        localCodeCheckPanel.clear()
    }

    /** 释放资源 */
    fun dispose() {
        coroutineScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        pacPipelinePanel.dispose()
    }
}
