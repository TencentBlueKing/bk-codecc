package com.codecc.preci.ui.settings

import com.codecc.preci.api.model.response.CheckerSet
import com.codecc.preci.api.model.response.ProjectInfo
import com.codecc.preci.core.config.PreCISettings
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.auth.AuthService
import com.codecc.preci.service.auth.GetCurrentProjectResult
import com.codecc.preci.service.auth.ProjectListResult
import com.codecc.preci.service.auth.SetProjectResult
import com.codecc.preci.service.scan.ScanService
import com.codecc.preci.service.scan.InitPhase
import com.codecc.preci.service.scan.InitResult
import com.codecc.preci.service.checker.CheckerService
import com.codecc.preci.service.checker.CheckerSetListResult
import com.codecc.preci.service.checker.CheckerSetSelectResult
import com.codecc.preci.service.server.ServerManagementService
import com.codecc.preci.service.server.ServerStartResult
import com.codecc.preci.service.server.ServerStartupActivity
import com.codecc.preci.service.version.VersionService
import com.codecc.preci.service.version.UpdateCheckResult
import com.codecc.preci.service.version.UpdateResult
import com.codecc.preci.util.CheckerSetConfigReader
import com.codecc.preci.util.ShellCommandHelper
import com.codecc.preci.util.TaskInfoConfigHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JTextField
import javax.swing.SwingUtilities

/**
 * PreCI 配置界面 UI 组件
 *
 * 负责创建和管理配置界面的所有 UI 控件，提供蓝盾项目选择和代码扫描规则集选择功能。
 *
 * **配置项：**
 * 1. **蓝盾项目**：下拉单选框，展示当前用户有权限的所有蓝盾项目
 * 2. **代码扫描规则集**：下拉多选框，展示 PreCI 提供的所有规则集
 *
 * **数据来源：**
 * - 蓝盾项目：通过 `/auth/list/projects` 接口获取
 * - 规则集：通过 `/checker/set/list` 接口获取
 * - 已选择的规则集：从项目目录 `.codecc/checkerset/` 读取
 *
 * @since 1.0
 */
class PreCISettingsComponent {

    private val logger = PreCILogger.getLogger(PreCISettingsComponent::class.java)
    private val panel: JPanel

    // ========== PreCI CLI 路径 UI 控件 ==========

    /**
     * PreCI CLI 路径输入框（带 Browse 按钮）
     */
    private val preciPathField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            "选择 PreCI CLI",
            "选择 PreCI CLI 可执行文件路径",
            null,
            FileChooserDescriptorFactory.createSingleFileDescriptor()
        )
    }

    /**
     * 检测更新按钮（原"自动检测"按钮，功能变更为检查 PreCI CLI 是否有可用更新）
     */
    private val checkUpdateButton = JButton("检测更新")

    /**
     * 路径检测状态标签
     */
    private val pathStatusLabel = JBLabel("").apply {
        foreground = java.awt.Color(128, 128, 128)
    }

    // ========== UI 控件 ==========

    /**
     * 蓝盾项目下拉框
     */
    private val projectComboBox = ComboBox<ProjectComboBoxItem>().apply {
        preferredSize = Dimension(400, preferredSize.height)
    }

    /**
     * 规则集复选框容器面板
     */
    private val checkerSetPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    /**
     * 规则集滚动面板
     */
    private val checkerSetScrollPane = JBScrollPane(checkerSetPanel).apply {
        preferredSize = Dimension(400, 200)
    }

    /**
     * 规则集复选框映射（ID -> CheckBox）
     */
    private val checkerSetCheckBoxes = mutableMapOf<String, JBCheckBox>()

    // ========== 白名单/黑名单 UI 控件 ==========

    /**
     * 白名单路径输入框（英文逗号分隔）
     */
    private val whitePathsField = JTextField().apply {
        toolTipText = "输入白名单路径，多个路径用英文逗号分隔"
    }

    /**
     * 黑名单路径输入框（英文逗号分隔）
     */
    private val blackPathsField = JTextField().apply {
        toolTipText = "输入黑名单路径，多个路径用英文逗号分隔"
    }

    // ========== Git 钩子 UI 控件 ==========

    /**
     * Pre-Commit 检查启用复选框
     */
    private val preCommitCheckBox = JBCheckBox("启用 Pre-Commit 检查").apply {
        toolTipText = "在 Git Commit 前自动执行 PreCI 代码扫描"
    }

    /**
     * Pre-Commit 扫描范围：变更文件
     */
    private val preCommitChangedFilesRadio = JRadioButton("变更文件代码检查（仅扫描暂存区变更）")

    /**
     * Pre-Commit 扫描范围：全部文件
     */
    private val preCommitAllFilesRadio = JRadioButton("全部文件代码检查")

    /**
     * Pre-Commit 扫描范围单选按钮组
     */
    private val preCommitScopeGroup = ButtonGroup().apply {
        add(preCommitChangedFilesRadio)
        add(preCommitAllFilesRadio)
    }

    /**
     * Pre-Push 检查启用复选框
     */
    private val prePushCheckBox = JBCheckBox("启用 Pre-Push 检查").apply {
        toolTipText = "在 Git Push 前自动执行 PreCI 代码扫描"
    }

    /**
     * Pre-Push 扫描范围：变更文件
     */
    private val prePushChangedFilesRadio = JRadioButton("变更文件代码检查（仅扫描未推送变更）")

    /**
     * Pre-Push 扫描范围：全部文件
     */
    private val prePushAllFilesRadio = JRadioButton("全部文件代码检查")

    /**
     * Pre-Push 扫描范围单选按钮组
     */
    private val prePushScopeGroup = ButtonGroup().apply {
        add(prePushChangedFilesRadio)
        add(prePushAllFilesRadio)
    }

    // ========== 加载/状态 UI 控件 ==========

    /**
     * 加载状态提示标签
     */
    private val loadingLabel = JBLabel("正在加载...")

    /**
     * 警告提示标签（用于显示项目未绑定等警告信息）
     */
    private val warningLabel = JBLabel().apply {
        foreground = java.awt.Color(255, 153, 0) // 橙色警告色
        isVisible = false
    }

    // ========== 数据缓存 ==========

    /**
     * 项目列表数据
     */
    private var projects: List<ProjectInfo> = emptyList()

    /**
     * 规则集列表数据
     */
    private var checkerSets: List<CheckerSet> = emptyList()

    /**
     * 初始加载的项目 ID（用于检测是否修改）
     */
    private var initialProjectId: String = ""

    /**
     * 初始加载的规则集 ID 列表（用于检测是否修改）
     */
    private var initialCheckerSets: Set<String> = emptySet()

    /**
     * 当前绑定的项目 ID（从 Local Server 获取）
     */
    private var currentBoundProjectId: String = ""

    /**
     * 初始加载的白名单路径（用于检测是否修改）
     */
    private var initialWhitePaths: String = ""

    /**
     * 初始加载的黑名单路径（用于检测是否修改）
     */
    private var initialBlackPaths: String = ""

    init {
        // 初始化 PreCI CLI 路径（从 Settings 读取）
        val settings = PreCISettings.getInstance()
        preciPathField.text = settings.preciPath

        // 路径输入框 + 检测更新按钮 水平排列
        val pathPanel = JPanel(BorderLayout(5, 0))
        pathPanel.add(preciPathField, BorderLayout.CENTER)
        pathPanel.add(checkUpdateButton, BorderLayout.EAST)

        // 检测更新按钮点击事件
        checkUpdateButton.addActionListener { handleCheckUpdate() }

        // 初始化 Git 钩子 UI 状态
        initGitHookControls(settings)

        // 构建 Git 钩子面板
        val gitHookPanel = buildGitHookPanel()

        // 构建表单布局
        panel = FormBuilder.createFormBuilder()
            .addComponent(warningLabel)
            .addLabeledComponent("PreCI CLI 路径:", pathPanel)
            .addComponent(pathStatusLabel)
            .addSeparator()
            .addLabeledComponent("蓝盾项目:", projectComboBox)
            .addLabeledComponent("代码扫描规则集:", checkerSetScrollPane)
            .addSeparator()
            .addLabeledComponent("白名单路径:", whitePathsField)
            .addLabeledComponent("黑名单路径:", blackPathsField)
            .addSeparator()
            .addComponent(JBLabel("Git 钩子检查").apply {
                font = font.deriveFont(java.awt.Font.BOLD)
            })
            .addComponent(gitHookPanel)
            .addComponent(loadingLabel)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        // 添加项目选择监听器
        projectComboBox.addActionListener {
            onProjectSelected()
        }

        // 初始化加载数据
        loadData()
    }

    /**
     * 初始化 Git 钩子控件状态（从 Settings 读取）
     */
    private fun initGitHookControls(settings: PreCISettings) {
        preCommitCheckBox.isSelected = settings.preCommitCheckEnabled
        preCommitChangedFilesRadio.isSelected = settings.preCommitScanScope == 102
        preCommitAllFilesRadio.isSelected = settings.preCommitScanScope == 0

        prePushCheckBox.isSelected = settings.prePushCheckEnabled
        prePushChangedFilesRadio.isSelected = settings.prePushScanScope == 103
        prePushAllFilesRadio.isSelected = settings.prePushScanScope == 0

        updateGitHookRadioState()

        preCommitCheckBox.addActionListener { updateGitHookRadioState() }
        prePushCheckBox.addActionListener { updateGitHookRadioState() }
    }

    /**
     * 根据复选框状态启用/禁用对应的单选按钮
     */
    private fun updateGitHookRadioState() {
        preCommitChangedFilesRadio.isEnabled = preCommitCheckBox.isSelected
        preCommitAllFilesRadio.isEnabled = preCommitCheckBox.isSelected
        prePushChangedFilesRadio.isEnabled = prePushCheckBox.isSelected
        prePushAllFilesRadio.isEnabled = prePushCheckBox.isSelected
    }

    /**
     * 构建 Git 钩子设置面板
     */
    private fun buildGitHookPanel(): JPanel {
        val gitPanel = JPanel()
        gitPanel.layout = BoxLayout(gitPanel, BoxLayout.Y_AXIS)
        gitPanel.border = BorderFactory.createEmptyBorder(5, 10, 5, 0)

        gitPanel.add(preCommitCheckBox)
        val preCommitScopePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(0, 25, 5, 0)
            add(preCommitChangedFilesRadio)
            add(preCommitAllFilesRadio)
        }
        gitPanel.add(preCommitScopePanel)

        gitPanel.add(Box.createVerticalStrut(5))

        gitPanel.add(prePushCheckBox)
        val prePushScopePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(0, 25, 5, 0)
            add(prePushChangedFilesRadio)
            add(prePushAllFilesRadio)
        }
        gitPanel.add(prePushScopePanel)

        return gitPanel
    }

    /**
     * 自动检测 PreCI CLI 路径并验证版本
     *
     * 保留此方法供其他模块调用（如 [ServerStartupActivity] 中的自动检测逻辑）。
     * Settings 页面的按钮已切换为 [handleCheckUpdate]。
     */
    @Suppress("unused")
    private fun handleAutoDetect() {
        checkUpdateButton.isEnabled = false
        checkUpdateButton.text = "检测中..."
        pathStatusLabel.text = ""

        CoroutineScope(Dispatchers.IO).launch {
            val result = ShellCommandHelper.detectPreCI()
            SwingUtilities.invokeLater {
                when (result.status) {
                    ShellCommandHelper.DetectionStatus.READY -> {
                        preciPathField.text = result.path ?: ""
                        pathStatusLabel.text = "检测到 PreCI CLI v2"
                        pathStatusLabel.foreground = java.awt.Color(0, 128, 0)
                    }
                    ShellCommandHelper.DetectionStatus.OLD_VERSION -> {
                        preciPathField.text = result.path ?: ""
                        pathStatusLabel.text = "检测到旧版本 PreCI，需要升级到 v2（${result.message}）"
                        pathStatusLabel.foreground = java.awt.Color(255, 153, 0)
                    }
                    ShellCommandHelper.DetectionStatus.NOT_FOUND -> {
                        pathStatusLabel.text = "未检测到 PreCI CLI，请手动指定路径或安装"
                        pathStatusLabel.foreground = java.awt.Color(200, 0, 0)
                    }
                }
                checkUpdateButton.text = "检测更新"
                checkUpdateButton.isEnabled = true
            }
        }
    }

    /**
     * 检查 PreCI CLI 更新
     *
     * 比较本地版本和线上最新版本，如果发现新版本则提示用户确认后执行更新。
     *
     * **流程：**
     * 1. 调用 [VersionService.checkForUpdate] 比较版本
     * 2. 如果有更新，弹出确认对话框
     * 3. 用户确认后调用 [VersionService.performUpdate] 执行更新
     */
    private fun handleCheckUpdate() {
        checkUpdateButton.isEnabled = false
        checkUpdateButton.text = "检测中..."
        pathStatusLabel.text = ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val versionService = VersionService.getInstance()
                val checkResult = versionService.checkForUpdate()

                SwingUtilities.invokeLater {
                    when (checkResult) {
                        is UpdateCheckResult.AlreadyLatest -> {
                            pathStatusLabel.text = "已是最新版本: ${checkResult.currentVersion}"
                            pathStatusLabel.foreground = java.awt.Color(0, 128, 0)
                            resetCheckUpdateButton()
                        }
                        is UpdateCheckResult.UpdateAvailable -> {
                            pathStatusLabel.text = "发现新版本: ${checkResult.latestVersion}（当前: ${checkResult.currentVersion}）"
                            pathStatusLabel.foreground = java.awt.Color(255, 153, 0)

                            val confirm = javax.swing.JOptionPane.showConfirmDialog(
                                panel,
                                "发现新版本 ${checkResult.latestVersion}（当前版本: ${checkResult.currentVersion}），是否立即更新？",
                                "PreCI CLI 更新",
                                javax.swing.JOptionPane.YES_NO_OPTION,
                                javax.swing.JOptionPane.QUESTION_MESSAGE
                            )

                            if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                                performUpdateInBackground(versionService)
                            } else {
                                resetCheckUpdateButton()
                            }
                        }
                        is UpdateCheckResult.Failure -> {
                            pathStatusLabel.text = "检测更新失败: ${checkResult.message}"
                            pathStatusLabel.foreground = java.awt.Color(200, 0, 0)
                            resetCheckUpdateButton()
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("检测更新异常: ${e.message}", e)
                SwingUtilities.invokeLater {
                    pathStatusLabel.text = "检测更新异常: ${e.message}"
                    pathStatusLabel.foreground = java.awt.Color(200, 0, 0)
                    resetCheckUpdateButton()
                }
            }
        }
    }

    /**
     * 在后台执行 PreCI CLI 更新并更新 UI 状态
     *
     * **完整流程：**
     * 1. 执行 `preci update`（同步等待 updater 完成）
     * 2. 轮询等待新版本 Local Server 就绪（updater 已自动启动新服务）
     * 3. 调用 [verifyUpdateVersion] 确认版本确实已更新
     *
     * @param versionService 版本服务实例
     */
    private fun performUpdateInBackground(versionService: VersionService) {
        checkUpdateButton.text = "更新中..."
        pathStatusLabel.text = "正在更新 PreCI CLI，请稍候..."
        pathStatusLabel.foreground = java.awt.Color(128, 128, 128)

        CoroutineScope(Dispatchers.IO).launch {
            val updateResult = versionService.performUpdate()

            when (updateResult) {
                is UpdateResult.Success -> {
                    SwingUtilities.invokeLater {
                        pathStatusLabel.text = "更新完成，正在等待服务就绪..."
                        pathStatusLabel.foreground = java.awt.Color(128, 128, 128)
                    }

                    val serverReady = waitForServerReady()
                    if (!serverReady) {
                        SwingUtilities.invokeLater {
                            pathStatusLabel.text = "更新完成，但服务启动失败，请手动执行 preci server start"
                            pathStatusLabel.foreground = java.awt.Color(255, 153, 0)
                            resetCheckUpdateButton()
                        }
                        return@launch
                    }

                    val versionVerified = verifyUpdateVersion(versionService)
                    SwingUtilities.invokeLater {
                        if (versionVerified) {
                            pathStatusLabel.text = "更新成功，服务已启动"
                            pathStatusLabel.foreground = java.awt.Color(0, 128, 0)
                        } else {
                            pathStatusLabel.text = "更新完成，但版本验证失败，请检查日志"
                            pathStatusLabel.foreground = java.awt.Color(255, 153, 0)
                        }
                        resetCheckUpdateButton()
                    }
                }
                is UpdateResult.Failure -> {
                    SwingUtilities.invokeLater {
                        pathStatusLabel.text = "更新失败: ${updateResult.message}"
                        pathStatusLabel.foreground = java.awt.Color(200, 0, 0)
                        resetCheckUpdateButton()
                    }
                }
            }
        }
    }

    /**
     * 轮询等待 Local Server 就绪（updater 会自动启动新版本服务）
     *
     * 最多等待 10 秒；若超时则尝试手动启动一次作为兜底。
     *
     * @return true 服务已就绪，false 服务未就绪
     */
    private suspend fun waitForServerReady(): Boolean {
        val openProjects = ProjectManager.getInstance().openProjects
        val realProject = openProjects.firstOrNull { !it.isDefault && it.basePath != null }
        if (realProject == null) {
            logger.warn("没有打开的项目，无法检测 Local Server 状态")
            return false
        }

        val serverService = ServerManagementService.getInstance(realProject)
        val maxRetries = 10
        for (i in 1..maxRetries) {
            if (serverService.isServerRunning()) {
                logger.info("更新后服务已就绪（第 ${i} 次检测）")
                return true
            }
            kotlinx.coroutines.delay(1000)
        }

        // 超时兜底：手动启动一次
        logger.warn("等待服务就绪超时，尝试手动启动")
        return when (serverService.startServer()) {
            is ServerStartResult.Success -> {
                logger.info("手动启动服务成功")
                true
            }
            is ServerStartResult.Failure -> {
                logger.error("手动启动服务失败")
                false
            }
        }
    }

    /**
     * 验证更新后版本是否确实已变更
     *
     * 通过再次调用 [VersionService.checkForUpdate]，若返回 [UpdateCheckResult.AlreadyLatest]
     * 则说明版本已成功更新。
     *
     * @return true 版本已更新，false 版本未更新或检查失败
     */
    private suspend fun verifyUpdateVersion(versionService: VersionService): Boolean {
        return try {
            when (versionService.checkForUpdate()) {
                is UpdateCheckResult.AlreadyLatest -> true
                is UpdateCheckResult.UpdateAvailable -> {
                    logger.warn("更新后版本检测仍显示有可用更新")
                    false
                }
                is UpdateCheckResult.Failure -> {
                    logger.error("更新后版本检测失败")
                    false
                }
            }
        } catch (e: Exception) {
            logger.error("更新后版本验证异常: ${e.message}", e)
            false
        }
    }

    /**
     * 重置检测更新按钮到初始状态
     */
    private fun resetCheckUpdateButton() {
        checkUpdateButton.text = "检测更新"
        checkUpdateButton.isEnabled = true
    }

    /**
     * 加载数据（项目列表、当前绑定项目和规则集列表）
     *
     * 先等待 PreCI Server 就绪，再发起 API 请求，避免在服务启动期间因连接失败弹出错误通知。
     */
    private fun loadData() {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                // 获取实际打开的项目（project 级别的服务无法从 defaultProject 获取）
                val openProjects = ProjectManager.getInstance().openProjects
                val realProject = openProjects.firstOrNull { !it.isDefault && it.basePath != null }
                if (realProject == null) {
                    logger.warn("没有打开的项目，无法加载蓝盾项目和规则集数据")
                    SwingUtilities.invokeLater {
                        loadingLabel.text = "请先打开一个项目后再配置"
                    }
                    return@launch
                }

                // 等待 PreCI Server 就绪（最多 30 秒）
                if (!ServerStartupActivity.serverReady.isCompleted) {
                    SwingUtilities.invokeLater {
                        loadingLabel.text = "正在等待 PreCI Server 就绪..."
                    }
                    logger.info("Waiting for PreCI Server to be ready before loading settings data...")
                }

                val serverIsReady = ServerStartupActivity.awaitServerReady(30_000L)
                if (!serverIsReady) {
                    logger.warn("PreCI Server 未就绪，尝试直接加载数据（可能触发自动启动）")
                    SwingUtilities.invokeLater {
                        loadingLabel.text = "PreCI Server 未就绪，正在尝试加载..."
                    }
                }

                val authService = AuthService.getInstance()

                // 1. 加载项目列表
                when (val projectResult = authService.getProjects()) {
                    is ProjectListResult.Success -> {
                        projects = projectResult.projects
                        logger.info("成功加载项目列表，共 ${projects.size} 个项目")
                    }
                    is ProjectListResult.Failure -> {
                        logger.error("加载项目列表失败: ${projectResult.message}")
                        projects = emptyList()
                    }
                }

                // 2. 获取当前绑定的项目
                when (val currentProjectResult = authService.getCurrentProject()) {
                    is GetCurrentProjectResult.Success -> {
                        currentBoundProjectId = currentProjectResult.projectId
                        logger.info("成功获取当前绑定项目: $currentBoundProjectId")
                    }
                    is GetCurrentProjectResult.Failure -> {
                        logger.error("获取当前绑定项目失败: ${currentProjectResult.message}")
                        currentBoundProjectId = ""
                        
                        SwingUtilities.invokeLater {
                            warningLabel.text = "⚠️ 蓝盾项目未绑定，请选择一个项目后点击 Apply 保存"
                            warningLabel.isVisible = true
                            panel.revalidate()
                            panel.repaint()
                            logger.info("已显示项目未绑定警告标签")
                        }
                    }
                }

                // 3. 加载规则集列表
                val checkerService = CheckerService.getInstance(realProject)
                when (val checkerResult = checkerService.getCheckerSetList()) {
                    is CheckerSetListResult.Success -> {
                        checkerSets = checkerResult.response.checkerSets
                        logger.info("成功加载规则集列表，共 ${checkerSets.size} 个规则集")
                    }
                    is CheckerSetListResult.Failure -> {
                        logger.error("加载规则集列表失败: ${checkerResult.message}")
                        checkerSets = emptyList()
                    }
                }

                // 4. 在 EDT 线程更新界面
                logger.info("准备更新 UI，项目数：${projects.size}，规则集数：${checkerSets.size}")
                SwingUtilities.invokeAndWait {
                    try {
                        logger.info("SwingUtilities.invokeAndWait 开始执行")
                        updateUI()
                        loadingLabel.isVisible = false
                        logger.info("UI 更新完成")
                    } catch (e: Exception) {
                        logger.error("更新 UI 失败: ${e.message}", e)
                        loadingLabel.text = "UI 更新失败: ${e.message}"
                    }
                }

            } catch (e: Exception) {
                logger.error("加载数据失败: ${e.message}", e)
                SwingUtilities.invokeLater {
                    loadingLabel.text = "加载失败: ${e.message}"
                }
            }
        }
    }

    /**
     * 更新 UI（填充项目和规则集数据）
     */
    private fun updateUI() {
        logger.info("updateUI 开始执行")

        // 更新项目下拉框
        val projectModel = DefaultComboBoxModel<ProjectComboBoxItem>()
        projectModel.addElement(ProjectComboBoxItem(null, "请选择项目..."))
        var selectedIndex = 0 // 默认选中"请选择项目..."
        projects.forEachIndexed { index, project ->
            projectModel.addElement(ProjectComboBoxItem(project, project.projectName))
            // 如果当前绑定的项目 ID 匹配，记录索引（需要 +1 因为第一个是占位符）
            if (currentBoundProjectId.isNotEmpty() && project.projectId == currentBoundProjectId) {
                selectedIndex = index + 1
            }
        }
        projectComboBox.model = projectModel

        // 设置选中当前绑定的项目
        if (selectedIndex > 0) {
            projectComboBox.selectedIndex = selectedIndex
            initialProjectId = currentBoundProjectId
            logger.info("已选中当前绑定项目: $currentBoundProjectId (index: $selectedIndex)")
        }
        logger.info("项目下拉框已更新，共 ${projectModel.size} 项")

        // 更新规则集复选框
        checkerSetPanel.removeAll()
        checkerSetCheckBoxes.clear()
        logger.info("清空规则集面板")

        checkerSets.forEach { checkerSet: CheckerSet ->
            val displayText = "${checkerSet.checkerSetName} (${checkerSet.toolName})"
            val checkBox = JBCheckBox(displayText)
            checkerSetCheckBoxes[checkerSet.checkerSetId] = checkBox
            checkerSetPanel.add(checkBox)
            logger.info("添加规则集复选框: $displayText")
        }

        logger.info("规则集面板已更新，共 ${checkerSetCheckBoxes.size} 个复选框")

        // 加载已选择的规则集（从 .codecc 目录读取）
        loadSelectedCheckerSetsFromConfig()

        // 加载白名单/黑名单路径（从 .codecc/taskInfo.json 读取）
        loadPathFiltersFromTaskInfo()

        // 强制刷新 UI
        checkerSetPanel.revalidate()
        checkerSetPanel.repaint()
        checkerSetScrollPane.revalidate()
        checkerSetScrollPane.repaint()
        panel.revalidate()
        panel.repaint()

        logger.info("UI 刷新完成")
    }

    /**
     * 从项目配置目录加载已选择的规则集
     *
     * 读取项目目录下 `.codecc/checkerset/` 文件夹中的 JSON 文件名，
     * 根据文件名（去掉 .json 后缀）判断哪些规则集已被选择，并在复选框中打勾。
     */
    private fun loadSelectedCheckerSetsFromConfig() {
        // 获取打开的项目列表
        val openProjects = ProjectManager.getInstance().openProjects
        if (openProjects.isEmpty()) {
            logger.warn("没有打开的项目，无法读取规则集配置")
            return
        }

        // 使用第一个打开的项目作为项目根目录
        val projectRootPath = openProjects[0].basePath
        if (projectRootPath == null) {
            logger.warn("项目根目录为空，无法读取规则集配置")
            return
        }

        // 读取已选择的规则集
        val selectedSets = CheckerSetConfigReader.getSelectedCheckerSets(projectRootPath)
        logger.info("从配置目录读取到已选择的规则集: $selectedSets")

        // 在复选框中选中这些规则集
        checkerSetCheckBoxes.forEach { (id, checkBox) ->
            val isSelected = selectedSets.contains(id)
            checkBox.isSelected = isSelected
            if (isSelected) {
                logger.info("规则集 $id 已被选中")
            }
        }

        // 记录初始选择状态
        initialCheckerSets = selectedSets.toSet()
        logger.info("初始选择的规则集数量: ${initialCheckerSets.size}")
    }

    /**
     * 从 taskInfo.json 加载白名单/黑名单路径并填充到 UI
     */
    private fun loadPathFiltersFromTaskInfo() {
        val openProjects = ProjectManager.getInstance().openProjects
        val projectRootPath = openProjects.firstOrNull { !it.isDefault && it.basePath != null }?.basePath
        if (projectRootPath == null) {
            logger.warn("没有打开的项目，无法读取白名单/黑名单配置")
            return
        }

        val pathFilters = TaskInfoConfigHelper.readPathFilters(projectRootPath)
        val whiteText = TaskInfoConfigHelper.joinToCommaSeparated(pathFilters.whitePaths)
        val blackText = TaskInfoConfigHelper.joinToCommaSeparated(pathFilters.blackPaths)

        whitePathsField.text = whiteText
        blackPathsField.text = blackText
        initialWhitePaths = whiteText
        initialBlackPaths = blackText

        logger.info("已加载白名单/黑名单: white='$whiteText', black='$blackText'")
    }

    /**
     * 项目选择监听器
     */
    private fun onProjectSelected() {
        val selectedItem = projectComboBox.selectedItem as? ProjectComboBoxItem ?: return
        val project = selectedItem.project ?: return

        logger.info("用户选择了项目: ${project.projectId}")

        // 隐藏警告标签（用户已选择项目）
        if (warningLabel.isVisible) {
            warningLabel.isVisible = false
            panel.revalidate()
            panel.repaint()
            logger.info("用户选择了项目，隐藏警告标签")
        }

        // 加载该项目的已选择规则集
        ApplicationManager.getApplication().invokeLater {
            loadProjectCheckerSets(project.projectId)
        }
    }

    /**
     * 加载项目的已选择规则集
     *
     * @param projectId 项目 ID
     */
    private fun loadProjectCheckerSets(projectId: String) {
        // 获取打开的项目列表
        val openProjects = ProjectManager.getInstance().openProjects
        if (openProjects.isEmpty()) {
            logger.warn("没有打开的项目，无法读取规则集配置")
            return
        }

        // 使用第一个打开的项目作为项目根目录
        val projectRootPath = openProjects[0].basePath ?: return
        val selectedSets = CheckerSetConfigReader.getSelectedCheckerSets(projectRootPath)

        logger.info("项目 $projectId 已选择规则集: $selectedSets")

        // 在复选框中选中这些规则集
        checkerSetCheckBoxes.forEach { (id, checkBox) ->
            checkBox.isSelected = selectedSets.contains(id)
        }
    }

    /**
     * 获取配置面板
     *
     * @return 配置面板的根组件
     */
    fun getPanel(): JComponent = panel

    /**
     * 检查配置是否被修改
     *
     * **判断逻辑：**
     * 1. 如果 server 端没有绑定项目（currentBoundProjectId 为空），只要用户选择了项目就算修改
     * 2. 如果用户选择的项目和初始项目不同，算修改
     * 3. 如果规则集发生变化，算修改
     *
     * @param settings 当前保存的配置
     * @return true 如果 UI 中的值与保存的配置不同，false 否则
     */
    fun isModified(settings: PreCISettings): Boolean {
        // 检查 PreCI CLI 路径是否变更
        if (preciPathField.text != settings.preciPath) {
            logger.info("isModified: preciPath 已变更 ('${settings.preciPath}' -> '${preciPathField.text}')，返回 true")
            return true
        }

        // 检查项目是否变更
        val selectedItem = projectComboBox.selectedItem as? ProjectComboBoxItem
        val currentProjectId = selectedItem?.project?.projectId ?: ""
        
        logger.info("isModified 检查: currentProjectId='$currentProjectId', initialProjectId='$initialProjectId', currentBoundProjectId='$currentBoundProjectId'")
        
        // 如果 server 端没有绑定项目，只要用户选择了有效项目就认为已修改
        if (currentBoundProjectId.isEmpty() && currentProjectId.isNotEmpty()) {
            logger.info("isModified: server 端未绑定项目，用户选择了有效项目 '$currentProjectId'，返回 true")
            return true
        }
        
        // 检查项目是否变更
        if (currentProjectId != initialProjectId) {
            logger.info("isModified: 项目已变更 ('$initialProjectId' -> '$currentProjectId')，返回 true")
            return true
        }

        // 检查规则集是否变更
        val currentCheckerSets = checkerSetCheckBoxes
            .filter { (_, checkBox) -> checkBox.isSelected }
            .map { (id, _) -> id }
            .toSet()

        logger.info("isModified 检查: currentCheckerSets=$currentCheckerSets, initialCheckerSets=$initialCheckerSets")
        
        val checkerSetsModified = currentCheckerSets != initialCheckerSets
        if (checkerSetsModified) {
            logger.info("isModified: 规则集已变更，返回 true")
            return true
        }

        // 检查白名单/黑名单是否变更
        if (whitePathsField.text != initialWhitePaths) {
            logger.info("isModified: 白名单已变更 ('$initialWhitePaths' -> '${whitePathsField.text}')，返回 true")
            return true
        }
        if (blackPathsField.text != initialBlackPaths) {
            logger.info("isModified: 黑名单已变更 ('$initialBlackPaths' -> '${blackPathsField.text}')，返回 true")
            return true
        }

        // 检查 Git 钩子配置是否变更
        if (preCommitCheckBox.isSelected != settings.preCommitCheckEnabled) return true
        if (getPreCommitScanScope() != settings.preCommitScanScope) return true
        if (prePushCheckBox.isSelected != settings.prePushCheckEnabled) return true
        if (getPrePushScanScope() != settings.prePushScanScope) return true

        logger.info("isModified: 无变更，返回 false")
        return false
    }

    /**
     * 从 UI 获取 pre-commit 扫描范围 scanType
     */
    private fun getPreCommitScanScope(): Int {
        return if (preCommitAllFilesRadio.isSelected) 0 else 102
    }

    /**
     * 从 UI 获取 pre-push 扫描范围 scanType
     */
    private fun getPrePushScanScope(): Int {
        return if (prePushAllFilesRadio.isSelected) 0 else 103
    }

    /**
     * 应用配置
     *
     * 将 UI 中的值保存到配置服务和 Local Server，并执行项目初始化。
     *
     * **执行流程：**
     * 1. 保存并设置蓝盾项目
     * 2. 保存并应用规则集
     * 3. 执行项目初始化（调用 `/task/init`）
     *
     * **注意：**
     * - 此方法使用 ProgressManager 在后台线程执行，显示进度条
     * - 会阻塞等待完成，确保配置被正确保存
     * - 即使项目未变化，也会调用 setProject 接口以确保 server 端状态正确
     *
     * @param settings 配置服务实例
     */
    fun apply(settings: PreCISettings) {
        logger.info("apply 方法被调用")
        
        // 使用 ProgressManager 显示进度条并在后台线程执行
        ProgressManager.getInstance().run(object : Task.Modal(null, "正在保存 PreCI 配置...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    logger.info("开始应用配置")
                    indicator.text = "正在应用配置..."
                    indicator.fraction = 0.0

                    // 保存 PreCI CLI 路径
                    settings.preciPath = preciPathField.text
                    logger.info("PreCI CLI path saved: ${settings.preciPath}")

                    // 保存 Git 钩子配置
                    settings.preCommitCheckEnabled = preCommitCheckBox.isSelected
                    settings.preCommitScanScope = getPreCommitScanScope()
                    settings.prePushCheckEnabled = prePushCheckBox.isSelected
                    settings.prePushScanScope = getPrePushScanScope()
                    logger.info("Git hook settings saved: preCommit=${settings.preCommitCheckEnabled}/${ settings.preCommitScanScope}, prePush=${settings.prePushCheckEnabled}/${settings.prePushScanScope}")

                    // 获取打开的项目列表（过滤掉 defaultProject，它没有 basePath）
                    val openProjects = ProjectManager.getInstance().openProjects
                    val realProject = openProjects.firstOrNull { !it.isDefault && it.basePath != null }
                    val projectRootPath = realProject?.basePath
                    
                    logger.info("获取项目: openProjects=${openProjects.size}, realProject=${realProject?.name}, projectRootPath=$projectRootPath")

                    // 1. 保存并设置项目
                    val selectedItem = projectComboBox.selectedItem as? ProjectComboBoxItem
                    val project = selectedItem?.project
                    if (project != null && realProject != null) {
                        logger.info("准备设置项目: ${project.projectId} (projectName: ${project.projectName})")
                        indicator.text = "正在设置项目: ${project.projectName}..."
                        indicator.fraction = 0.2
                        
                        settings.currentProjectId = project.projectId

                        val authService = AuthService.getInstance()
                        runBlocking {
                            logger.info("调用 authService.setProject: ${project.projectId}")
                            when (val result = authService.setProject(project.projectId)) {
                                is SetProjectResult.Success -> {
                                    logger.info("成功设置项目: ${project.projectId}")
                                }
                                is SetProjectResult.Failure -> {
                                    logger.error("设置项目失败: ${result.message}")
                                }
                            }
                        }
                    } else {
                        logger.info("未选择项目或没有打开的项目，跳过设置项目步骤")
                    }

                    // 2. 保存并应用规则集
                    val selectedCheckerSetIds = checkerSetCheckBoxes
                        .filter { (_, checkBox) -> checkBox.isSelected }
                        .map { (id, _) -> id }
                    logger.info("准备选择规则集: $selectedCheckerSetIds")
                    indicator.text = "正在应用规则集配置..."
                    indicator.fraction = 0.5
                    
                    // 跟踪是否有影响 server 状态的配置变更，决定是否需要重新初始化
                    var needsInit = false

                    if (projectRootPath != null) {
                        val checkerService = CheckerService.getInstance(realProject)

                        val checkerSetsModified = selectedCheckerSetIds.toSet() != initialCheckerSets
                        if (checkerSetsModified) {
                            needsInit = true
                            runBlocking {
                                when (val result = checkerService.selectCheckerSets(selectedCheckerSetIds, projectRootPath)) {
                                    is CheckerSetSelectResult.Success -> {
                                        logger.info("成功选择规则集: $selectedCheckerSetIds")
                                    }
                                    is CheckerSetSelectResult.Failure -> {
                                        logger.error("选择规则集失败: ${result.message}")
                                    }
                                }
                            }

                            val unselectedCheckerSetIds = initialCheckerSets.filter { it !in selectedCheckerSetIds }
                            if (unselectedCheckerSetIds.isNotEmpty()) {
                                indicator.text = "正在取消未选择的规则集..."
                                indicator.fraction = 0.6
                                runBlocking {
                                    when (val result = checkerService.unselectCheckerSets(unselectedCheckerSetIds, projectRootPath)) {
                                        is CheckerSetSelectResult.Success -> {
                                            logger.info("成功取消选择规则集: $unselectedCheckerSetIds")
                                        }
                                        is CheckerSetSelectResult.Failure -> {
                                            logger.error("取消选择规则集失败: ${result.message}")
                                        }
                                    }
                                }
                            }
                        } else {
                            logger.info("规则集未变更，跳过规则集配置")
                        }

                        // 3. 如果白名单/黑名单有修改，写入 taskInfo.json
                        val whitePathsModified = whitePathsField.text != initialWhitePaths
                        val blackPathsModified = blackPathsField.text != initialBlackPaths
                        if (whitePathsModified || blackPathsModified) {
                            needsInit = true
                            indicator.text = "正在更新白名单/黑名单配置..."
                            indicator.fraction = 0.7
                            val whitePaths = TaskInfoConfigHelper.parseCommaSeparated(whitePathsField.text)
                            val blackPaths = TaskInfoConfigHelper.parseCommaSeparated(blackPathsField.text)
                            val writeSuccess = TaskInfoConfigHelper.writePathFilters(projectRootPath, whitePaths, blackPaths)
                            if (writeSuccess) {
                                logger.info("成功写入白名单/黑名单到 taskInfo.json")
                                initialWhitePaths = whitePathsField.text
                                initialBlackPaths = blackPathsField.text
                            } else {
                                logger.error("写入白名单/黑名单到 taskInfo.json 失败")
                            }
                        }

                        // 项目变更也需要重新初始化
                        val selectedProjectId = (projectComboBox.selectedItem as? ProjectComboBoxItem)?.project?.projectId ?: ""
                        if (selectedProjectId != initialProjectId) {
                            needsInit = true
                        }

                        if (needsInit) {
                            indicator.text = "正在初始化项目..."
                            indicator.fraction = 0.8

                            val scanService = ScanService.getInstance(realProject)
                            runBlocking {
                                val initResult = scanService.initProject(projectRootPath) { progress ->
                                    when (progress.phase) {
                                        InitPhase.INITIALIZING -> {
                                            indicator.isIndeterminate = true
                                            indicator.text = "正在初始化项目配置..."
                                            indicator.text2 = ""
                                        }
                                        InitPhase.DOWNLOADING_TOOL -> {
                                            indicator.isIndeterminate = false
                                            val toolFraction = 0.8 + 0.18 * progress.toolIndex / progress.totalTools
                                            indicator.fraction = toolFraction
                                            indicator.text = "正在下载工具 (${progress.toolIndex}/${progress.totalTools})..."
                                            indicator.text2 = progress.currentTool ?: ""
                                        }
                                        InitPhase.COMPLETED -> {
                                            indicator.isIndeterminate = false
                                            indicator.fraction = 0.98
                                            indicator.text = "工具下载完成"
                                            indicator.text2 = ""
                                        }
                                    }
                                }
                                when (initResult) {
                                    is InitResult.Success -> {
                                        logger.info("✅ 项目初始化成功")
                                    }
                                    is InitResult.Failure -> {
                                        logger.error("❌ 项目初始化失败: ${initResult.message}")
                                    }
                                }
                            }
                        } else {
                            logger.info("无需重新初始化项目（项目、规则集、路径过滤均未变更）")
                        }
                    } else {
                        logger.warn("没有打开有效的项目（或项目根路径为空），跳过规则集和初始化步骤")
                    }

                    // 更新初始值状态
                    initialProjectId = settings.currentProjectId
                    initialCheckerSets = selectedCheckerSetIds.toSet()
                    
                    indicator.text = "配置保存完成"
                    indicator.fraction = 1.0
                    logger.info("✅ 配置应用完成")

                } catch (e: Exception) {
                    logger.error("❌ 应用配置失败: ${e.message}", e)
                }
            }
        })
    }

    /**
     * 重置配置
     *
     * 将 UI 重置为当前保存的配置值
     *
     * @param settings 配置服务实例
     */
    fun reset(settings: PreCISettings) {
        // 重置 PreCI CLI 路径
        preciPathField.text = settings.preciPath
        pathStatusLabel.text = ""

        // 重置 Git 钩子设置
        preCommitCheckBox.isSelected = settings.preCommitCheckEnabled
        preCommitChangedFilesRadio.isSelected = settings.preCommitScanScope == 102
        preCommitAllFilesRadio.isSelected = settings.preCommitScanScope == 0
        prePushCheckBox.isSelected = settings.prePushCheckEnabled
        prePushChangedFilesRadio.isSelected = settings.prePushScanScope == 103
        prePushAllFilesRadio.isSelected = settings.prePushScanScope == 0
        updateGitHookRadioState()

        // 重置项目选择
        initialProjectId = settings.currentProjectId
        if (initialProjectId.isNotEmpty()) {
            for (i in 0 until projectComboBox.itemCount) {
                val item = projectComboBox.getItemAt(i)
                if (item.project?.projectId == initialProjectId) {
                    projectComboBox.selectedIndex = i
                    break
                }
            }
        } else {
            projectComboBox.selectedIndex = 0 // 选择"请选择项目..."
        }

        // 重置规则集选择和白名单/黑名单
        val openProjects = ProjectManager.getInstance().openProjects
        val realProject = openProjects.firstOrNull { !it.isDefault && it.basePath != null }
        val projectRootPath = realProject?.basePath
        if (projectRootPath != null) {
            val selectedSets = CheckerSetConfigReader.getSelectedCheckerSets(projectRootPath)
            initialCheckerSets = selectedSets.toSet()
            checkerSetCheckBoxes.forEach { (id, checkBox) ->
                checkBox.isSelected = selectedSets.contains(id)
            }

            // 重置白名单/黑名单
            val pathFilters = TaskInfoConfigHelper.readPathFilters(projectRootPath)
            val whiteText = TaskInfoConfigHelper.joinToCommaSeparated(pathFilters.whitePaths)
            val blackText = TaskInfoConfigHelper.joinToCommaSeparated(pathFilters.blackPaths)
            whitePathsField.text = whiteText
            blackPathsField.text = blackText
            initialWhitePaths = whiteText
            initialBlackPaths = blackText
        }
    }
}

/**
 * 项目下拉框选项数据类
 *
 * @property project 项目信息，如果为 null 表示"请选择项目..."占位符
 * @property displayText 显示文本
 */
private data class ProjectComboBoxItem(
    val project: ProjectInfo?,
    val displayText: String
) {
    override fun toString(): String = displayText
}
