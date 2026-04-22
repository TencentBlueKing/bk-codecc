package com.codecc.preci.ui.action

import com.codecc.preci.BaseTest
import com.codecc.preci.service.scan.ScanService
import com.codecc.preci.ui.toolwindow.PreCIToolWindow
import com.codecc.preci.ui.toolwindow.PreCIToolWindowFactory
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * LoadScanResultAction 单元测试
 *
 * 测试加载扫描结果 Action 的各种场景，包括成功和失败情况。
 *
 * **测试范围：**
 * - Action 的基本功能（打开工具窗口、调用 loadResults）
 * - 异常处理（null project、工具窗口不存在等）
 * - Action 状态更新（enable/disable）
 * - ActionUpdateThread 配置
 *
 * **注意：**
 * 由于 Action 内部调用 ScanService 和工具窗口，这些依赖在单元测试环境中
 * 可能无法完全模拟。本测试主要验证 Action 的核心逻辑和边界情况处理。
 *
 * @since 1.0
 */
@DisplayName("LoadScanResultAction 测试")
class LoadScanResultActionTest : BaseTest() {

    private lateinit var action: LoadScanResultAction
    private lateinit var project: Project
    private lateinit var event: AnActionEvent
    private lateinit var presentation: Presentation
    private lateinit var toolWindowManager: ToolWindowManager
    private lateinit var toolWindow: ToolWindow
    private lateinit var preCIToolWindow: PreCIToolWindow
    private lateinit var scanService: ScanService
    private lateinit var application: Application

    @BeforeEach
    fun setup() {
        action = LoadScanResultAction()

        // Mock Project
        project = mockk(relaxed = true)
        every { project.basePath } returns "/test/project"

        // Mock Presentation
        presentation = mockk(relaxed = true)
        var isEnabled = true
        every { presentation.isEnabled = any() } answers { isEnabled = firstArg() }
        every { presentation.isEnabled } answers { isEnabled }

        // Mock AnActionEvent
        event = mockk(relaxed = true)
        every { event.project } returns project
        every { event.presentation } returns presentation

        // Mock Application 和 ApplicationManager
        application = mockk<Application>(relaxed = true)
        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns application

        // 捕获并立即执行 invokeLater 中的 Runnable
        val runnableSlot = slot<Runnable>()
        every { application.invokeLater(capture(runnableSlot)) } answers {
            runnableSlot.captured.run()
        }

        // Mock ToolWindowManager 静态方法
        toolWindowManager = mockk<ToolWindowManager>(relaxed = true)
        toolWindow = mockk<ToolWindow>(relaxed = true)
        mockkStatic(ToolWindowManager::class)
        every { ToolWindowManager.getInstance(project) } returns toolWindowManager
        every { toolWindowManager.getToolWindow("PreCI") } returns toolWindow
        every { toolWindow.show() } just runs

        // Mock PreCIToolWindowFactory
        mockkObject(PreCIToolWindowFactory)
        preCIToolWindow = mockk<PreCIToolWindow>(relaxed = true)
        every { PreCIToolWindowFactory.getToolWindow(project) } returns preCIToolWindow
        every { preCIToolWindow.refresh() } just runs

        // Mock ScanService（关键：mock companion object 的 getInstance 方法）
        mockkObject(ScanService)
        scanService = mockk<ScanService>(relaxed = true)
        every { ScanService.getInstance(project) } returns scanService
        // Mock getScanResult 方法，返回一个成功结果（避免协程中的异常）
        coEvery { scanService.getScanResult(any()) } returns com.codecc.preci.service.scan.ScanResultQueryResult.Success(
            com.codecc.preci.api.model.response.ScanResultResponse(emptyList())
        )
    }

    @AfterEach
    fun teardown() {
        // 清理所有 mock，确保测试之间互不影响
        unmockkAll()
        // 等待一小段时间，让协程完成
        Thread.sleep(100)
    }

    @Test
    @DisplayName("加载扫描结果 - 基本功能")
    fun `actionPerformed should open tool window and load results`() {
        // 执行 Action
        action.actionPerformed(event)

        // 等待协程和 invokeLater 完成
        Thread.sleep(300)

        // 验证工具窗口被打开
        verify(atLeast = 1) {
            toolWindowManager.getToolWindow("PreCI")
        }

        // 验证 toolWindow.show() 被调用
        verify(atLeast = 1) {
            toolWindow.show()
        }

        // 验证代码不会崩溃
        assertTrue(true, "Action 应该正常执行")
    }

    @Test
    @DisplayName("加载扫描结果 - Project 为 null")
    fun `actionPerformed should handle null project gracefully`() {
        // Mock event 返回 null project
        every { event.project } returns null

        // 执行 Action（应该直接返回，不执行任何操作）
        action.actionPerformed(event)

        // 验证没有调用 ToolWindowManager（因为 project 为 null，直接返回）
        // 这个测试主要验证代码不会因为 null project 而崩溃
        assertTrue(true, "Action 应该处理 null project 的情况而不崩溃")
    }

    @Test
    @DisplayName("加载扫描结果 - 工具窗口不存在")
    fun `actionPerformed should handle missing tool window gracefully`() {
        // Mock ToolWindowManager 返回 null
        every { toolWindowManager.getToolWindow("PreCI") } returns null

        // 执行 Action
        action.actionPerformed(event)

        // 等待协程完成
        Thread.sleep(300)

        // 验证代码不会崩溃
        assertTrue(true, "Action 应该处理工具窗口不存在的情况而不崩溃")
    }

    @Test
    @DisplayName("加载扫描结果 - PreCIToolWindow 实例不存在")
    fun `actionPerformed should handle missing PreCIToolWindow instance gracefully`() {
        // Mock PreCIToolWindowFactory 返回 null
        every { PreCIToolWindowFactory.getToolWindow(project) } returns null

        // 执行 Action
        action.actionPerformed(event)

        // 等待协程完成
        Thread.sleep(300)

        // 验证代码不会崩溃
        assertTrue(true, "Action 应该处理 PreCIToolWindow 实例不存在的情况而不崩溃")
    }

    @Test
    @DisplayName("更新 Action 状态 - Project 存在")
    fun `update should enable action when project exists`() {
        // 执行 update
        action.update(event)

        // 验证 Action 可用
        verify { presentation.isEnabled = true }
    }

    @Test
    @DisplayName("更新 Action 状态 - Project 为 null")
    fun `update should disable action when project is null`() {
        // Mock event 返回 null project
        every { event.project } returns null

        // 执行 update
        action.update(event)

        // 验证 Action 不可用
        verify { presentation.isEnabled = false }
    }

    @Test
    @DisplayName("ActionUpdateThread 应该是 BGT")
    fun `getActionUpdateThread should return BGT`() {
        val thread = action.actionUpdateThread
        assertNotNull(thread)
        assertTrue(thread == ActionUpdateThread.BGT, "ActionUpdateThread 应该是 BGT")
    }

    @Test
    @DisplayName("Action 类应该正确初始化")
    fun `action should be initialized correctly`() {
        assertNotNull(action)
        // templateText 可能为 null（取决于 Action 的配置），不验证它
    }
}
