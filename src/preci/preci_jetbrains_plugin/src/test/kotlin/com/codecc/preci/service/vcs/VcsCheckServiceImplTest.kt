package com.codecc.preci.service.vcs

import com.codecc.preci.BaseTest
import com.codecc.preci.api.model.response.Defect
import com.codecc.preci.api.model.response.ScanProgressResponse
import com.codecc.preci.api.model.response.ScanResponse
import com.codecc.preci.api.model.response.ScanResultResponse
import com.codecc.preci.core.config.PreCISettings
import com.codecc.preci.service.scan.*
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * VcsCheckServiceImpl 单元测试
 *
 * 测试 VCS 检查服务在各种场景下的行为，包括：
 * - 功能禁用时直接放行
 * - 变更文件扫描和全量扫描的正确路由
 * - 扫描成功/有缺陷/无缺陷的结果处理
 * - 各种失败场景（扫描失败、进度查询失败、结果查询失败）
 *
 * @since 1.1
 */
@DisplayName("VcsCheckServiceImpl 测试")
class VcsCheckServiceImplTest : BaseTest() {

    private lateinit var project: Project
    private lateinit var vcsCheckService: VcsCheckServiceImpl
    private lateinit var mockScanService: ScanService
    private lateinit var mockSettings: PreCISettings

    @BeforeEach
    fun setup() {
        project = mockk(relaxed = true)
        every { project.basePath } returns "/test/project"

        mockScanService = mockk()
        mockSettings = mockk(relaxed = true)

        // Mock ApplicationManager 以拦截 PreCISettings.getInstance()
        val mockApplication = mockk<Application>(relaxed = true)
        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns mockApplication
        every { mockApplication.getService(PreCISettings::class.java) } returns mockSettings

        // Mock project.getService 以拦截 ScanService.getInstance(project)
        every { project.getService(ScanService::class.java) } returns mockScanService

        vcsCheckService = VcsCheckServiceImpl(project)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    // ========== Pre-Commit 检查测试 ==========

    @Nested
    @DisplayName("Pre-Commit 检查测试")
    inner class PreCommitCheckTests {

        @Test
        @DisplayName("Pre-Commit 检查未启用，返回 Disabled")
        fun `should return Disabled when pre-commit check is not enabled`() = runTest {
            every { mockSettings.preCommitCheckEnabled } returns false

            val result = vcsCheckService.performPreCommitCheck()

            assertTrue(result is VcsCheckResult.Disabled)
            coVerify(exactly = 0) { mockScanService.preCommitScan(any()) }
            coVerify(exactly = 0) { mockScanService.fullScan(any()) }
        }

        @Test
        @DisplayName("Pre-Commit 变更文件扫描 - 无缺陷")
        fun `should return NoDefects for changed files scan with no defects`() = runTest {
            every { mockSettings.preCommitCheckEnabled } returns true
            every { mockSettings.preCommitScanScope } returns 102

            coEvery { mockScanService.preCommitScan(any()) } returns ScanResult.Success(
                ScanResponse(message = "Scan started", tools = listOf("lint"), scanFileNum = 5)
            )
            coEvery { mockScanService.getScanProgress() } returns ScanProgressResult.Success(
                ScanProgressResponse(projectRoot = "/test", toolStatuses = mapOf("lint" to "done"), status = "done")
            )
            coEvery { mockScanService.getScanResult(any()) } returns ScanResultQueryResult.Success(
                ScanResultResponse(defects = emptyList())
            )

            val result = vcsCheckService.performPreCommitCheck()

            assertTrue(result is VcsCheckResult.NoDefects)
            coVerify(exactly = 1) { mockScanService.preCommitScan(any()) }
        }

        @Test
        @DisplayName("Pre-Commit 变更文件扫描 - 发现缺陷")
        fun `should return HasDefects for changed files scan with defects`() = runTest {
            every { mockSettings.preCommitCheckEnabled } returns true
            every { mockSettings.preCommitScanScope } returns 102

            val defects = listOf(
                Defect("lint", "errcheck", "unchecked error", "/test/main.go", 42),
                Defect("lint", "unused", "unused var", "/test/utils.go", 10)
            )

            coEvery { mockScanService.preCommitScan(any()) } returns ScanResult.Success(
                ScanResponse(message = "Scan started", tools = listOf("lint"), scanFileNum = 5)
            )
            coEvery { mockScanService.getScanProgress() } returns ScanProgressResult.Success(
                ScanProgressResponse(projectRoot = "/test", toolStatuses = mapOf("lint" to "done"), status = "done")
            )
            coEvery { mockScanService.getScanResult(any()) } returns ScanResultQueryResult.Success(
                ScanResultResponse(defects = defects)
            )

            val result = vcsCheckService.performPreCommitCheck()

            assertTrue(result is VcsCheckResult.HasDefects)
            val hasDefects = result as VcsCheckResult.HasDefects
            assertEquals(2, hasDefects.defectCount)
            assertEquals(2, hasDefects.defects.size)
        }

        @Test
        @DisplayName("Pre-Commit 全量扫描 - 调用 fullScan")
        fun `should call fullScan when preCommitScanScope is 0`() = runTest {
            every { mockSettings.preCommitCheckEnabled } returns true
            every { mockSettings.preCommitScanScope } returns 0

            coEvery { mockScanService.fullScan(any()) } returns ScanResult.Success(
                ScanResponse(message = "Scan started", tools = listOf("lint"), scanFileNum = 100)
            )
            coEvery { mockScanService.getScanProgress() } returns ScanProgressResult.Success(
                ScanProgressResponse(projectRoot = "/test", toolStatuses = mapOf("lint" to "done"), status = "done")
            )
            coEvery { mockScanService.getScanResult(any()) } returns ScanResultQueryResult.Success(
                ScanResultResponse(defects = emptyList())
            )

            val result = vcsCheckService.performPreCommitCheck()

            assertTrue(result is VcsCheckResult.NoDefects)
            coVerify(exactly = 1) { mockScanService.fullScan(any()) }
            coVerify(exactly = 0) { mockScanService.preCommitScan(any()) }
        }

        @Test
        @DisplayName("Pre-Commit 扫描启动失败 - 返回 Error")
        fun `should return Error when scan fails to start`() = runTest {
            every { mockSettings.preCommitCheckEnabled } returns true
            every { mockSettings.preCommitScanScope } returns 102

            coEvery { mockScanService.preCommitScan(any()) } returns ScanResult.Failure(
                message = "服务器未运行", exception = RuntimeException("Connection refused")
            )

            val result = vcsCheckService.performPreCommitCheck()

            assertTrue(result is VcsCheckResult.Error)
            val error = result as VcsCheckResult.Error
            assertTrue(error.message.contains("扫描启动失败"))
        }

        @Test
        @DisplayName("Pre-Commit 进度查询失败 - 返回 Error")
        fun `should return Error when progress query fails`() = runTest {
            every { mockSettings.preCommitCheckEnabled } returns true
            every { mockSettings.preCommitScanScope } returns 102

            coEvery { mockScanService.preCommitScan(any()) } returns ScanResult.Success(
                ScanResponse(message = "Scan started", tools = listOf("lint"), scanFileNum = 5)
            )
            coEvery { mockScanService.getScanProgress() } returns ScanProgressResult.Failure(
                message = "网络错误", exception = RuntimeException("timeout")
            )

            val result = vcsCheckService.performPreCommitCheck()

            assertTrue(result is VcsCheckResult.Error)
            val error = result as VcsCheckResult.Error
            assertTrue(error.message.contains("进度查询失败"))
        }

        @Test
        @DisplayName("Pre-Commit 结果查询失败 - 返回 Error")
        fun `should return Error when result query fails`() = runTest {
            every { mockSettings.preCommitCheckEnabled } returns true
            every { mockSettings.preCommitScanScope } returns 102

            coEvery { mockScanService.preCommitScan(any()) } returns ScanResult.Success(
                ScanResponse(message = "Scan started", tools = listOf("lint"), scanFileNum = 5)
            )
            coEvery { mockScanService.getScanProgress() } returns ScanProgressResult.Success(
                ScanProgressResponse(projectRoot = "/test", toolStatuses = mapOf("lint" to "done"), status = "done")
            )
            coEvery { mockScanService.getScanResult(any()) } returns ScanResultQueryResult.Failure(
                message = "未登录或认证已过期"
            )

            val result = vcsCheckService.performPreCommitCheck()

            assertTrue(result is VcsCheckResult.Error)
            val error = result as VcsCheckResult.Error
            assertTrue(error.message.contains("结果查询失败"))
        }

        @Test
        @DisplayName("Pre-Commit 扫描进度轮询 - 等待直到完成")
        fun `should poll progress until scan is done`() = runTest {
            every { mockSettings.preCommitCheckEnabled } returns true
            every { mockSettings.preCommitScanScope } returns 102

            coEvery { mockScanService.preCommitScan(any()) } returns ScanResult.Success(
                ScanResponse(message = "Scan started", tools = listOf("lint"), scanFileNum = 5)
            )

            var callCount = 0
            coEvery { mockScanService.getScanProgress() } answers {
                callCount++
                if (callCount < 3) {
                    ScanProgressResult.Success(
                        ScanProgressResponse(projectRoot = "/test", toolStatuses = mapOf("lint" to "running"), status = "running")
                    )
                } else {
                    ScanProgressResult.Success(
                        ScanProgressResponse(projectRoot = "/test", toolStatuses = mapOf("lint" to "done"), status = "done")
                    )
                }
            }
            coEvery { mockScanService.getScanResult(any()) } returns ScanResultQueryResult.Success(
                ScanResultResponse(defects = emptyList())
            )

            val result = vcsCheckService.performPreCommitCheck()

            assertTrue(result is VcsCheckResult.NoDefects)
            coVerify(atLeast = 3) { mockScanService.getScanProgress() }
        }
    }

    // ========== Pre-Push 检查测试 ==========

    @Nested
    @DisplayName("Pre-Push 检查测试")
    inner class PrePushCheckTests {

        @Test
        @DisplayName("Pre-Push 检查未启用，返回 Disabled")
        fun `should return Disabled when pre-push check is not enabled`() = runTest {
            every { mockSettings.prePushCheckEnabled } returns false

            val result = vcsCheckService.performPrePushCheck()

            assertTrue(result is VcsCheckResult.Disabled)
            coVerify(exactly = 0) { mockScanService.prePushScan(any()) }
        }

        @Test
        @DisplayName("Pre-Push 变更文件扫描 - 无缺陷")
        fun `should return NoDefects for changed files push scan`() = runTest {
            every { mockSettings.prePushCheckEnabled } returns true
            every { mockSettings.prePushScanScope } returns 103

            coEvery { mockScanService.prePushScan(any()) } returns ScanResult.Success(
                ScanResponse(message = "Scan started", tools = listOf("lint"), scanFileNum = 8)
            )
            coEvery { mockScanService.getScanProgress() } returns ScanProgressResult.Success(
                ScanProgressResponse(projectRoot = "/test", toolStatuses = mapOf("lint" to "done"), status = "done")
            )
            coEvery { mockScanService.getScanResult(any()) } returns ScanResultQueryResult.Success(
                ScanResultResponse(defects = emptyList())
            )

            val result = vcsCheckService.performPrePushCheck()

            assertTrue(result is VcsCheckResult.NoDefects)
            coVerify(exactly = 1) { mockScanService.prePushScan(any()) }
        }

        @Test
        @DisplayName("Pre-Push 变更文件扫描 - 发现缺陷")
        fun `should return HasDefects for push scan with defects`() = runTest {
            every { mockSettings.prePushCheckEnabled } returns true
            every { mockSettings.prePushScanScope } returns 103

            val defects = listOf(
                Defect("gosec", "G104", "Audit errors not checked", "/test/utils.go", 15)
            )

            coEvery { mockScanService.prePushScan(any()) } returns ScanResult.Success(
                ScanResponse(message = "Scan started", tools = listOf("gosec"), scanFileNum = 3)
            )
            coEvery { mockScanService.getScanProgress() } returns ScanProgressResult.Success(
                ScanProgressResponse(projectRoot = "/test", toolStatuses = mapOf("gosec" to "done"), status = "done")
            )
            coEvery { mockScanService.getScanResult(any()) } returns ScanResultQueryResult.Success(
                ScanResultResponse(defects = defects)
            )

            val result = vcsCheckService.performPrePushCheck()

            assertTrue(result is VcsCheckResult.HasDefects)
            val hasDefects = result as VcsCheckResult.HasDefects
            assertEquals(1, hasDefects.defectCount)
        }

        @Test
        @DisplayName("Pre-Push 全量扫描 - 调用 fullScan")
        fun `should call fullScan when prePushScanScope is 0`() = runTest {
            every { mockSettings.prePushCheckEnabled } returns true
            every { mockSettings.prePushScanScope } returns 0

            coEvery { mockScanService.fullScan(any()) } returns ScanResult.Success(
                ScanResponse(message = "Scan started", tools = listOf("lint"), scanFileNum = 100)
            )
            coEvery { mockScanService.getScanProgress() } returns ScanProgressResult.Success(
                ScanProgressResponse(projectRoot = "/test", toolStatuses = mapOf("lint" to "done"), status = "done")
            )
            coEvery { mockScanService.getScanResult(any()) } returns ScanResultQueryResult.Success(
                ScanResultResponse(defects = emptyList())
            )

            val result = vcsCheckService.performPrePushCheck()

            assertTrue(result is VcsCheckResult.NoDefects)
            coVerify(exactly = 1) { mockScanService.fullScan(any()) }
            coVerify(exactly = 0) { mockScanService.prePushScan(any()) }
        }

        @Test
        @DisplayName("Pre-Push 扫描启动失败 - 返回 Error")
        fun `should return Error when push scan fails to start`() = runTest {
            every { mockSettings.prePushCheckEnabled } returns true
            every { mockSettings.prePushScanScope } returns 103

            coEvery { mockScanService.prePushScan(any()) } returns ScanResult.Failure(
                message = "没有未推送的提交"
            )

            val result = vcsCheckService.performPrePushCheck()

            assertTrue(result is VcsCheckResult.Error)
            assertTrue((result as VcsCheckResult.Error).message.contains("扫描启动失败"))
        }

        @Test
        @DisplayName("Pre-Push 异常 - 返回 Error 不阻塞")
        fun `should return Error without blocking on unexpected exception`() = runTest {
            every { mockSettings.prePushCheckEnabled } returns true
            every { mockSettings.prePushScanScope } returns 103

            coEvery { mockScanService.prePushScan(any()) } throws RuntimeException("Unexpected error")

            val result = vcsCheckService.performPrePushCheck()

            assertTrue(result is VcsCheckResult.Error)
            assertTrue((result as VcsCheckResult.Error).message.contains("异常"))
        }
    }

    // ========== 未知 scanType 测试 ==========

    @Nested
    @DisplayName("边界场景测试")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("未知 scanType - 返回 Error")
        fun `should return Error for unknown scanType`() = runTest {
            every { mockSettings.preCommitCheckEnabled } returns true
            every { mockSettings.preCommitScanScope } returns 999

            val result = vcsCheckService.performPreCommitCheck()

            assertTrue(result is VcsCheckResult.Error)
            assertTrue((result as VcsCheckResult.Error).message.contains("未知"))
        }
    }
}
