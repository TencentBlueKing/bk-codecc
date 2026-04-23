package com.codecc.preci.ui.toolwindow

import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.scan.InitPhase
import com.codecc.preci.service.scan.InitResult
import com.codecc.preci.service.scan.ScanService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.*

/**
 * 初始化项目面板
 *
 * 提供 PreCI 项目初始化功能的 UI 面板。用户点击"初始化"按钮后：
 * 1. 调用 `POST /task/init` 接口初始化项目配置
 * 2. 对返回的每个工具调用 `GET /task/reload/tool/{toolName}` 下载
 *
 * 初始化过程中，按钮切换为进度条，进度信息直接显示在进度条文本上。
 *
 * @property project 当前项目实例
 * @property coroutineScope 协程作用域，用于执行异步操作
 * @since 1.0
 */
class InitProjectPanel(
    private val project: Project,
    private val coroutineScope: CoroutineScope
) {

    private val logger = PreCILogger.getLogger(InitProjectPanel::class.java)

    private companion object {
        private const val CARD_BUTTON = "button"
        private const val CARD_PROGRESS = "progress"
    }

    private val mainPanel: JPanel = JBPanel<JBPanel<*>>(BorderLayout())
    private val cardLayout = CardLayout()
    private val cardPanel = JPanel(cardLayout)

    private val initButton: JButton = JButton("初始化", IconLoader.getIcon("/icon/setup.svg", InitProjectPanel::class.java)).apply {
        toolTipText = "初始化 PreCI 项目配置"
        addActionListener { handleInitProject() }
    }

    private val progressBar: JProgressBar = JProgressBar(0, 100).apply {
        isStringPainted = true
        string = "正在初始化..."
        preferredSize = initButton.preferredSize
    }

    init {
        initializeLayout()
    }

    private fun initializeLayout() {
        mainPanel.border = JBUI.Borders.empty(0)

        mainPanel.add(initButton, BorderLayout.CENTER)

        cardPanel.add(initButton, CARD_BUTTON)
        cardPanel.add(progressBar, CARD_PROGRESS)
        cardLayout.show(cardPanel, CARD_BUTTON)

        mainPanel.add(cardPanel, BorderLayout.CENTER)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun handleInitProject() {
        logger.info("用户点击初始化按钮")

        SwingUtilities.invokeLater {
            progressBar.value = 0
            progressBar.isIndeterminate = true
            progressBar.string = "正在初始化..."
            cardLayout.show(cardPanel, CARD_PROGRESS)
        }

        coroutineScope.launch {
            try {
                val scanService = ScanService.getInstance(project)
                val result = scanService.initProject(onProgress = { progress ->
                    SwingUtilities.invokeLater {
                        when (progress.phase) {
                            InitPhase.INITIALIZING -> {
                                progressBar.isIndeterminate = true
                                progressBar.string = "正在初始化..."
                            }
                            InitPhase.DOWNLOADING_TOOL -> {
                                progressBar.isIndeterminate = false
                                val percent = (progress.toolIndex * 100) / progress.totalTools
                                progressBar.value = percent
                                progressBar.string = "下载工具: ${progress.currentTool} (${progress.toolIndex}/${progress.totalTools})"
                            }
                            InitPhase.COMPLETED -> {
                                progressBar.isIndeterminate = false
                                progressBar.value = 100
                                progressBar.string = "初始化完成"
                            }
                        }
                    }
                })

                SwingUtilities.invokeLater {
                    cardLayout.show(cardPanel, CARD_BUTTON)
                }

                when (result) {
                    is InitResult.Success -> {
                        logger.info("项目初始化成功: rootPath=${result.response.rootPath}")
                    }
                    is InitResult.Failure -> {
                        logger.error("项目初始化失败: ${result.message}", result.exception)
                    }
                }

            } catch (e: Exception) {
                logger.error("初始化项目异常: ${e.message}", e)
                SwingUtilities.invokeLater {
                    cardLayout.show(cardPanel, CARD_BUTTON)
                }
            }
        }
    }

    fun getContent(): JComponent {
        return mainPanel
    }
}

