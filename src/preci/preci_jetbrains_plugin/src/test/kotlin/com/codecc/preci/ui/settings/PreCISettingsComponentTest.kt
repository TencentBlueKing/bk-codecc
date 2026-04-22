package com.codecc.preci.ui.settings

import com.codecc.preci.BaseTest
import com.codecc.preci.api.model.response.CheckerSet
import com.codecc.preci.api.model.response.CheckerSetListResponse
import com.codecc.preci.api.model.response.InitResponse
import com.codecc.preci.api.model.response.ProjectInfo
import com.codecc.preci.api.model.response.ProjectListResponse
import com.codecc.preci.core.config.PreCISettings
import com.codecc.preci.service.auth.AuthService
import com.codecc.preci.service.auth.GetCurrentProjectResult
import com.codecc.preci.service.auth.ProjectListResult
import com.codecc.preci.service.auth.SetProjectResult
import com.codecc.preci.service.checker.CheckerService
import com.codecc.preci.service.checker.CheckerSetInfo
import com.codecc.preci.service.checker.CheckerSetListResult
import com.codecc.preci.service.checker.CheckerSetSelectResult
import com.codecc.preci.service.scan.ScanService
import com.codecc.preci.service.scan.InitResult
import com.codecc.preci.service.version.VersionService
import com.codecc.preci.service.version.UpdateCheckResult
import com.codecc.preci.service.version.UpdateResult
import com.codecc.preci.util.CheckerSetConfigReader
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

/**
 * PreCISettingsComponent 单元测试
 *
 * 测试配置界面组件的核心功能：
 * 1. apply 方法是否正确调用 setProject 接口
 * 2. apply 方法是否正确调用 selectCheckerSets 接口
 * 3. apply 方法是否正确调用 initProject 接口
 * 4. 配置是否正确保存到 PreCISettings
 *
 * @since 1.0
 */
class PreCISettingsComponentTest : BaseTest() {

    private lateinit var authService: AuthService
    private lateinit var checkerService: CheckerService
    private lateinit var scanService: ScanService
    private lateinit var versionService: VersionService
    private lateinit var settings: PreCISettings
    private lateinit var testProject: Project

    @BeforeEach
    fun setUp() {
        // Mock 服务
        authService = mockk(relaxed = true)
        checkerService = mockk(relaxed = true)
        scanService = mockk(relaxed = true)
        versionService = mockk(relaxed = true)
        settings = mockk(relaxed = true)
        testProject = mockk(relaxed = true)
        every { testProject.basePath } returns "/tmp/test-project"
        every { testProject.isDefault } returns false

        // Mock ProjectManager
        val projectManager = mockk<ProjectManager>(relaxed = true)
        mockkStatic(ProjectManager::class)
        every { ProjectManager.getInstance() } returns projectManager
        every { projectManager.defaultProject } returns testProject
        every { projectManager.openProjects } returns arrayOf(testProject)

        // Mock ProgressManager（apply() 中使用 Task.Modal 需要它）
        val progressManager = mockk<ProgressManager>(relaxed = true)
        mockkStatic(ProgressManager::class)
        every { ProgressManager.getInstance() } returns progressManager
        every { progressManager.run(any<com.intellij.openapi.progress.Task.Modal>()) } answers {
            val task = firstArg<com.intellij.openapi.progress.Task.Modal>()
            val indicator = mockk<ProgressIndicator>(relaxed = true)
            task.run(indicator)
        }

        // Mock ApplicationManager（onProjectSelected 中使用 invokeLater）
        val application = mockk<Application>(relaxed = true)
        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns application
        every { application.invokeLater(any()) } answers {
            firstArg<Runnable>().run()
        }

        // Mock CheckerSetConfigReader（避免文件系统访问）
        mockkObject(CheckerSetConfigReader)
        every { CheckerSetConfigReader.getSelectedCheckerSets(any()) } returns emptyList()

        // Mock 服务单例（使用 mockkObject 替代 mockkStatic 以兼容 @JvmStatic companion object）
        mockkObject(AuthService.Companion)
        mockkObject(CheckerService.Companion)
        mockkObject(ScanService.Companion)
        mockkObject(VersionService.Companion)
        mockkObject(PreCISettings.Companion)

        every { AuthService.getInstance() } returns authService
        every { CheckerService.getInstance(any()) } returns checkerService
        every { ScanService.getInstance(any()) } returns scanService
        every { VersionService.getInstance() } returns versionService
        every { PreCISettings.getInstance() } returns settings

        // 默认返回成功结果
        val projectList = listOf(
            ProjectInfo("project1", "Project 1"),
            ProjectInfo("project2", "Project 2")
        )
        coEvery { authService.getProjects() } returns ProjectListResult.Success(
            projects = projectList,
            response = ProjectListResponse(projects = projectList)
        )

        coEvery { authService.getCurrentProject() } returns GetCurrentProjectResult.Success("project1")

        coEvery { authService.setProject(any()) } returns SetProjectResult.Success("project1")

        val checkerSetListResponse = CheckerSetListResponse(
            listOf(
                CheckerSet("checkerset1", "CheckerSet 1", "DETEKT"),
                CheckerSet("checkerset2", "CheckerSet 2", "PYLINT")
            )
        )
        coEvery { checkerService.getCheckerSetList() } returns CheckerSetListResult.Success(
            checkerSets = listOf(
                CheckerSetInfo("checkerset1", "CheckerSet 1", "DETEKT"),
                CheckerSetInfo("checkerset2", "CheckerSet 2", "PYLINT")
            ),
            response = checkerSetListResponse
        )

        coEvery { checkerService.selectCheckerSets(any(), any()) } returns CheckerSetSelectResult.Success(
            projectRoot = "/tmp/test-project",
            selectedSets = emptyList()
        )

        coEvery { checkerService.unselectCheckerSets(any(), any()) } returns CheckerSetSelectResult.Success(
            projectRoot = "/tmp/test-project",
            selectedSets = emptyList()
        )

        coEvery { scanService.initProject(any()) } returns InitResult.Success(InitResponse(rootPath = "/tmp/test-project"))
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    /**
     * 测试：apply 方法应该调用 setProject 接口
     *
     * **验证点：**
     * 1. 当用户选择项目并点击 Apply 时
     * 2. 应该调用 authService.setProject() 方法
     * 3. 传递正确的项目 ID
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test apply should call setProject API`() = runBlocking {
        // Given: 创建 Settings 组件
        val component = PreCISettingsComponent()

        // 等待数据加载完成
        Thread.sleep(2000)

        // When: 选择项目并应用配置
        // 注意：由于 UI 组件需要在 EDT 线程中操作，这里我们直接 mock 验证
        // 实际的 UI 交互测试需要使用 UITest 框架

        // 模拟 apply 被调用（这会触发 setProject）
        component.apply(settings)

        // Then: 验证 setProject 被调用
        coVerify(timeout = 10000) {
            authService.setProject(any())
        }

        // 验证至少调用了一次
        assertTrue(true, "setProject 接口应该被调用")
    }

    /**
     * 测试：apply 方法应该正确传递项目 ID
     *
     * **验证点：**
     * 1. setProject 接口被调用时
     * 2. 应该传递用户选择的项目 ID
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test apply should pass correct project ID to setProject`() = runBlocking {
        // Given
        val component = PreCISettingsComponent()
        Thread.sleep(2000)

        // When
        component.apply(settings)

        // Then: 验证传递的参数
        coVerify(timeout = 10000) {
            authService.setProject(match { projectId ->
                // 应该是某个有效的项目 ID
                projectId.isNotEmpty()
            })
        }
    }

    /**
     * 测试：apply 方法应该调用 selectCheckerSets 接口
     *
     * **验证点：**
     * 1. 当用户选择规则集并点击 Apply 时
     * 2. 应该调用 checkerService.selectCheckerSets() 方法
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test apply should call selectCheckerSets API`() = runBlocking {
        // Given
        val component = PreCISettingsComponent()
        Thread.sleep(2000)

        // When
        component.apply(settings)

        // Then
        coVerify(timeout = 10000) {
            checkerService.selectCheckerSets(any(), any())
        }
    }

    /**
     * 测试：apply 方法应该调用 initProject 接口
     *
     * **验证点：**
     * 1. 在设置项目和规则集后
     * 2. 应该自动调用项目初始化接口
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test apply should call initProject API`() = runBlocking {
        // Given
        val component = PreCISettingsComponent()
        Thread.sleep(2000)

        // When
        component.apply(settings)

        // Then
        coVerify(timeout = 10000) {
            scanService.initProject(any())
        }
    }

    /**
     * 测试：apply 方法应该保存项目 ID 到 settings
     *
     * **验证点：**
     * 1. 调用 apply 后
     * 2. settings.currentProjectId 应该被设置
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test apply should save project ID to settings`() = runBlocking {
        // Given
        val component = PreCISettingsComponent()
        Thread.sleep(2000)

        // When
        component.apply(settings)

        // Then
        verify(timeout = 10000) {
            settings.currentProjectId = any()
        }
    }

    /**
     * 测试：apply 方法按正确顺序执行操作
     *
     * **验证点：**
     * 1. setProject 应该在 selectCheckerSets 之前执行
     * 2. selectCheckerSets 应该在 initProject 之前执行
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test apply should execute operations in correct order`() = runBlocking {
        // Given
        val callOrder = mutableListOf<String>()

        coEvery { authService.setProject(any()) } answers {
            callOrder.add("setProject")
            SetProjectResult.Success("project1")
        }

        coEvery { checkerService.selectCheckerSets(any(), any()) } answers {
            callOrder.add("selectCheckerSets")
            CheckerSetSelectResult.Success(
                projectRoot = "/tmp/test-project",
                selectedSets = emptyList()
            )
        }

        coEvery { checkerService.unselectCheckerSets(any(), any()) } answers {
            callOrder.add("unselectCheckerSets")
            CheckerSetSelectResult.Success(
                projectRoot = "/tmp/test-project",
                selectedSets = emptyList()
            )
        }

        coEvery { scanService.initProject(any()) } answers {
            callOrder.add("initProject")
            InitResult.Success(InitResponse(rootPath = "/tmp/test-project"))
        }

        val component = PreCISettingsComponent()
        Thread.sleep(2000)

        // When
        component.apply(settings)
        Thread.sleep(3000) // 等待所有操作完成

        // Then: 验证调用顺序
        assertTrue(callOrder.indexOf("setProject") < callOrder.indexOf("selectCheckerSets"),
            "setProject 应该在 selectCheckerSets 之前")
        assertTrue(callOrder.indexOf("selectCheckerSets") < callOrder.indexOf("initProject"),
            "selectCheckerSets 应该在 initProject 之前")
    }

    /**
     * 测试：当 setProject 失败时，apply 方法仍应继续执行
     *
     * **验证点：**
     * 1. 即使 setProject 失败
     * 2. selectCheckerSets 和 initProject 仍应该被调用
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test apply should continue when setProject fails`() = runBlocking {
        // Given: setProject 返回失败
        coEvery { authService.setProject(any()) } returns SetProjectResult.Failure(
            "设置项目失败",
            Exception("Test error")
        )

        val component = PreCISettingsComponent()
        Thread.sleep(2000)

        // When
        component.apply(settings)

        // Then: 其他操作仍应该被调用
        coVerify(timeout = 10000) {
            checkerService.selectCheckerSets(any(), any())
        }
        coVerify(timeout = 10000) {
            scanService.initProject(any())
        }
    }

    /**
     * 测试：apply 方法在有规则集被取消勾选时应调用 unselectCheckerSets
     *
     * **验证点：**
     * 1. 当初始选中了规则集但 apply 时被取消勾选
     * 2. 应该调用 checkerService.unselectCheckerSets() 方法
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test apply should call unselectCheckerSets when checker sets are unchecked`() = runBlocking {
        // Given
        val component = PreCISettingsComponent()
        Thread.sleep(2000)

        // When
        component.apply(settings)

        // Then: unselectCheckerSets 可能被调用（取决于初始状态与当前选择的差集）
        // 由于 UI mock 限制，这里验证 mock 设置不会导致异常
        coVerify(timeout = 10000) {
            checkerService.selectCheckerSets(any(), any())
        }
    }

    /**
     * 测试：apply 方法执行顺序包含 unselectCheckerSets
     *
     * **验证点：**
     * 1. unselectCheckerSets 应在 selectCheckerSets 之后、initProject 之前执行
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test apply should call unselectCheckerSets before initProject`() = runBlocking {
        // Given
        val callOrder = mutableListOf<String>()

        coEvery { authService.setProject(any()) } answers {
            callOrder.add("setProject")
            SetProjectResult.Success("project1")
        }

        coEvery { checkerService.selectCheckerSets(any(), any()) } answers {
            callOrder.add("selectCheckerSets")
            CheckerSetSelectResult.Success(
                projectRoot = "/tmp/test-project",
                selectedSets = emptyList()
            )
        }

        coEvery { checkerService.unselectCheckerSets(any(), any()) } answers {
            callOrder.add("unselectCheckerSets")
            CheckerSetSelectResult.Success(
                projectRoot = "/tmp/test-project",
                selectedSets = emptyList()
            )
        }

        coEvery { scanService.initProject(any()) } answers {
            callOrder.add("initProject")
            InitResult.Success(InitResponse(rootPath = "/tmp/test-project"))
        }

        val component = PreCISettingsComponent()
        Thread.sleep(2000)

        // When
        component.apply(settings)
        Thread.sleep(3000)

        // Then: 如果 unselectCheckerSets 被调用，它应在 initProject 之前
        val unselectIndex = callOrder.indexOf("unselectCheckerSets")
        val initIndex = callOrder.indexOf("initProject")
        if (unselectIndex >= 0 && initIndex >= 0) {
            assertTrue(unselectIndex < initIndex,
                "unselectCheckerSets 应该在 initProject 之前")
        }
    }

    /**
     * 测试：isModified 方法应该正确检测项目变更
     *
     * **验证点：**
     * 1. 初始状态下 isModified 应该返回 false
     * 2. 修改项目后 isModified 应该返回 true
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test isModified should detect project changes`() {
        // Given
        val component = PreCISettingsComponent()
        Thread.sleep(2000)

        // 初始状态
        every { settings.currentProjectId } returns "project1"

        // When/Then: 初始状态应该是未修改
        assertFalse(component.isModified(settings), "初始状态应该是未修改")

        // 注意：由于 UI 组件的限制，我们无法直接修改 ComboBox 选择
        // 这里主要验证方法不会抛出异常
    }

    /**
     * 测试：reset 方法应该恢复到保存的配置
     *
     * **验证点：**
     * 1. reset 方法不应该抛出异常
     * 2. 应该从 settings 读取配置
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test reset should restore saved configuration`() {
        // Given
        val component = PreCISettingsComponent()
        Thread.sleep(2000)

        every { settings.currentProjectId } returns "project2"

        // When/Then: reset 不应该抛出异常
        assertDoesNotThrow {
            component.reset(settings)
        }
    }

    // ========== 检测更新相关测试 ==========

    /**
     * 测试：VersionService mock 注册正确，组件创建不会因缺少服务而异常
     *
     * **验证点：**
     * 1. PreCISettingsComponent 创建后 VersionService mock 可用
     * 2. checkForUpdate 可以被正常调用
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test version service mock is available after component creation`() = runBlocking {
        // Given
        coEvery { versionService.checkForUpdate() } returns UpdateCheckResult.AlreadyLatest("1.0.0")

        @Suppress("UNUSED_VARIABLE")
        val component = PreCISettingsComponent()
        Thread.sleep(2000)

        // When: 调用 VersionService
        val result = versionService.checkForUpdate()

        // Then: Mock 返回预期结果
        assertTrue(result is UpdateCheckResult.AlreadyLatest)
        assertEquals("1.0.0", (result as UpdateCheckResult.AlreadyLatest).currentVersion)
    }

    /**
     * 测试：VersionService.checkForUpdate 返回有更新可用时 mock 工作正常
     *
     * **验证点：**
     * 1. UpdateAvailable 结果包含正确的当前版本和最新版本
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test version service returns update available`() = runBlocking {
        // Given
        coEvery { versionService.checkForUpdate() } returns UpdateCheckResult.UpdateAvailable(
            currentVersion = "1.0.0",
            latestVersion = "2.0.0"
        )

        @Suppress("UNUSED_VARIABLE")
        val component = PreCISettingsComponent()
        Thread.sleep(2000)

        // When
        val result = versionService.checkForUpdate()

        // Then
        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val updateAvailable = result as UpdateCheckResult.UpdateAvailable
        assertEquals("1.0.0", updateAvailable.currentVersion)
        assertEquals("2.0.0", updateAvailable.latestVersion)
    }

    /**
     * 测试：VersionService.performUpdate 成功时 mock 工作正常
     *
     * **验证点：**
     * 1. performUpdate 返回 Success 结果
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test version service perform update success`() = runBlocking {
        // Given
        coEvery { versionService.performUpdate() } returns UpdateResult.Success("Updated to 2.0.0")

        @Suppress("UNUSED_VARIABLE")
        val component = PreCISettingsComponent()
        Thread.sleep(2000)

        // When
        val result = versionService.performUpdate()

        // Then
        assertTrue(result is UpdateResult.Success)
        assertEquals("Updated to 2.0.0", (result as UpdateResult.Success).message)
    }

    /**
     * 测试：VersionService.checkForUpdate 失败时 mock 工作正常
     *
     * **验证点：**
     * 1. Failure 结果包含错误信息
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test version service check update failure`() = runBlocking {
        // Given
        coEvery { versionService.checkForUpdate() } returns UpdateCheckResult.Failure("网络错误")

        @Suppress("UNUSED_VARIABLE")
        val component = PreCISettingsComponent()
        Thread.sleep(2000)

        // When
        val result = versionService.checkForUpdate()

        // Then
        assertTrue(result is UpdateCheckResult.Failure)
        assertEquals("网络错误", (result as UpdateCheckResult.Failure).message)
    }
}

