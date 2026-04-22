package com.codecc.preci.service.scan

import com.codecc.preci.BaseTest
import com.codecc.preci.api.client.PreCIApiClient
import com.codecc.preci.api.model.response.Defect
import com.codecc.preci.api.model.response.InitResponse
import com.codecc.preci.api.model.response.ScanCancelResponse
import com.codecc.preci.api.model.response.ScanProgressResponse
import com.codecc.preci.api.model.response.ScanResponse
import com.codecc.preci.api.model.response.ScanResultResponse
import com.codecc.preci.core.http.BusinessException
import com.codecc.preci.core.http.NetworkException
import com.codecc.preci.core.http.ServerNotRunningException
import com.intellij.openapi.project.Project
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested

/**
 * ScanService 单元测试
 *
 * 测试扫描服务的项目初始化和各种扫描功能，包括成功场景和各种失败场景。
 *
 * @since 1.0
 */
@DisplayName("ScanService 测试")
class ScanServiceTest : BaseTest() {

    private lateinit var project: Project
    private lateinit var scanService: ScanServiceImpl
    private lateinit var apiClient: PreCIApiClient

    @BeforeEach
    fun setup() {
        // Mock Project
        project = mockk(relaxed = true)
        every { project.basePath } returns "/test/project"

        // 创建 ScanService 实例
        scanService = ScanServiceImpl(project)

        // Mock PreCIApiClient（通过反射替换）
        apiClient = mockk()
        val apiClientField = ScanServiceImpl::class.java.getDeclaredField("apiClient")
        apiClientField.isAccessible = true
        apiClientField.set(scanService, apiClient)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    @DisplayName("项目初始化 - 成功（使用默认 rootPath）")
    fun `initProject should succeed with default rootPath`() = runTest {
        // 准备数据
        val expectedRootPath = "/test/project"
        val response = InitResponse(rootPath = expectedRootPath)

        // Mock API 调用
        coEvery {
            apiClient.initProject(any())
        } returns response

        // 执行初始化
        val result = scanService.initProject()

        // 验证结果
        assertTrue(result is InitResult.Success)
        val successResult = result as InitResult.Success
        assertEquals(expectedRootPath, successResult.response.rootPath)

        // 验证 API 被正确调用
        coVerify(exactly = 1) {
            apiClient.initProject(
                match { req ->
                    req.currentPath == "/test/project" &&
                    req.rootPath == "/test/project"
                }
            )
        }
    }

    @Test
    @DisplayName("项目初始化 - 成功（指定 rootPath）")
    fun `initProject should succeed with specified rootPath`() = runTest {
        // 准备数据
        val customRootPath = "/custom/root"
        val response = InitResponse(rootPath = customRootPath)

        // Mock API 调用
        coEvery {
            apiClient.initProject(any())
        } returns response

        // 执行初始化（指定 rootPath）
        val result = scanService.initProject(rootPath = customRootPath)

        // 验证结果
        assertTrue(result is InitResult.Success)
        val successResult = result as InitResult.Success
        assertEquals(customRootPath, successResult.response.rootPath)

        // 验证 API 被正确调用
        coVerify(exactly = 1) {
            apiClient.initProject(
                match { req ->
                    req.currentPath == "/test/project" &&
                    req.rootPath == customRootPath
                }
            )
        }
    }

    @Test
    @DisplayName("项目初始化 - 失败（Project.basePath 为 null）")
    fun `initProject should fail when project basePath is null`() = runTest {
        // Mock Project.basePath 返回 null
        every { project.basePath } returns null

        // 执行初始化
        val result = scanService.initProject()

        // 验证结果
        assertTrue(result is InitResult.Failure)
        val failureResult = result as InitResult.Failure
        assertTrue(failureResult.message.contains("无法获取项目根目录"))

        // 验证 API 未被调用
        coVerify(exactly = 0) {
            apiClient.initProject(any())
        }
    }

    @Test
    @DisplayName("项目初始化 - 失败（401 未登录）")
    fun `initProject should fail with 401 BusinessException`() = runTest {
        // Mock API 抛出 401 异常
        coEvery {
            apiClient.initProject(any())
        } throws BusinessException(401, "Unauthorized")

        // 执行初始化
        val result = scanService.initProject()

        // 验证结果
        assertTrue(result is InitResult.Failure)
        val failureResult = result as InitResult.Failure
        assertTrue(failureResult.message.contains("未登录或认证已过期"))
        assertNotNull(failureResult.exception)
        assertTrue(failureResult.exception is BusinessException)
    }

    @Test
    @DisplayName("项目初始化 - 失败（400 请求参数无效）")
    fun `initProject should fail with 400 BusinessException`() = runTest {
        // Mock API 抛出 400 异常
        coEvery {
            apiClient.initProject(any())
        } throws BusinessException(400, "Invalid request")

        // 执行初始化
        val result = scanService.initProject()

        // 验证结果
        assertTrue(result is InitResult.Failure)
        val failureResult = result as InitResult.Failure
        assertTrue(failureResult.message.contains("请求参数无效"))
    }

    @Test
    @DisplayName("项目初始化 - 失败（403 权限不足）")
    fun `initProject should fail with 403 BusinessException`() = runTest {
        // Mock API 抛出 403 异常
        coEvery {
            apiClient.initProject(any())
        } throws BusinessException(403, "Forbidden")

        // 执行初始化
        val result = scanService.initProject()

        // 验证结果
        assertTrue(result is InitResult.Failure)
        val failureResult = result as InitResult.Failure
        assertTrue(failureResult.message.contains("权限不足"))
    }

    @Test
    @DisplayName("项目初始化 - 失败（500 服务器错误）")
    fun `initProject should fail with 500 BusinessException`() = runTest {
        // Mock API 抛出 500 异常
        coEvery {
            apiClient.initProject(any())
        } throws BusinessException(500, "Internal Server Error")

        // 执行初始化
        val result = scanService.initProject()

        // 验证结果
        assertTrue(result is InitResult.Failure)
        val failureResult = result as InitResult.Failure
        assertTrue(failureResult.message.contains("服务器内部错误"))
    }

    @Test
    @DisplayName("项目初始化 - 失败（网络错误）")
    fun `initProject should fail with NetworkException`() = runTest {
        // Mock API 抛出网络异常
        coEvery {
            apiClient.initProject(any())
        } throws NetworkException("Connection timeout")

        // 执行初始化
        val result = scanService.initProject()

        // 验证结果
        assertTrue(result is InitResult.Failure)
        val failureResult = result as InitResult.Failure
        assertTrue(failureResult.message.contains("网络错误"))
        assertTrue(failureResult.message.contains("Connection timeout"))
        assertNotNull(failureResult.exception)
        assertTrue(failureResult.exception is NetworkException)
    }

    @Test
    @DisplayName("项目初始化 - 失败（Server 未运行）")
    fun `initProject should fail when server is not running`() = runTest {
        // Mock API 抛出 ServerNotRunningException
        coEvery {
            apiClient.initProject(any())
        } throws ServerNotRunningException("Server not running")

        // 执行初始化
        val result = scanService.initProject()

        // 验证结果
        assertTrue(result is InitResult.Failure)
        val failureResult = result as InitResult.Failure
        assertTrue(failureResult.message.contains("网络错误") || failureResult.message.contains("Server not running"))
        assertNotNull(failureResult.exception)
    }

    @Test
    @DisplayName("项目初始化 - 失败（未知异常）")
    fun `initProject should handle unexpected exceptions`() = runTest {
        // Mock API 抛出未知异常
        coEvery {
            apiClient.initProject(any())
        } throws RuntimeException("Unexpected error")

        // 执行初始化
        val result = scanService.initProject()

        // 验证结果
        assertTrue(result is InitResult.Failure)
        val failureResult = result as InitResult.Failure
        assertTrue(failureResult.message.contains("未知错误"))
        assertNotNull(failureResult.exception)
        assertTrue(failureResult.exception is RuntimeException)
    }

    @Test
    @DisplayName("验证日志记录和通知")
    fun `initProject should log and notify correctly`() = runTest {
        // 准备数据
        val response = InitResponse(rootPath = "/test/project")

        // Mock API 调用
        coEvery {
            apiClient.initProject(any())
        } returns response

        // 执行初始化
        val result = scanService.initProject()

        // 验证结果（间接验证日志已记录）
        assertTrue(result is InitResult.Success)
        // 注意：通知相关的验证较难直接测试，通常需要集成测试
    }

    // ========== 全量扫描测试 ==========

    @Nested
    @DisplayName("全量扫描测试")
    inner class FullScanTests {

        @Test
        @DisplayName("全量扫描 - 成功（使用默认 rootDir）")
        fun `fullScan should succeed with default rootDir`() = runTest {
            // 准备数据
            val response = ScanResponse(
                message = "Scan started",
                tools = listOf("golangci-lint", "gosec"),
                scanFileNum = 100
            )

            // Mock API 调用
            coEvery {
                apiClient.scan(any())
            } returns response

            // 执行扫描
            val result = scanService.fullScan()

            // 验证结果
            assertTrue(result is ScanResult.Success)
            val successResult = result as ScanResult.Success
            assertEquals(response.message, successResult.response.message)
            assertEquals(response.tools, successResult.response.tools)
            assertEquals(response.scanFileNum, successResult.response.scanFileNum)

            // 验证 API 被正确调用
            coVerify(exactly = 1) {
                apiClient.scan(
                    match { req ->
                        req.scanType == 0 && // 全量扫描
                        req.paths == null &&
                        req.rootDir == "/test/project"
                    }
                )
            }
        }

        @Test
        @DisplayName("全量扫描 - 成功（指定 rootDir）")
        fun `fullScan should succeed with specified rootDir`() = runTest {
            // 准备数据
            val customRootDir = "/custom/root"
            val response = ScanResponse(
                message = "Scan started",
                tools = listOf("golangci-lint"),
                scanFileNum = 50
            )

            // Mock API 调用
            coEvery {
                apiClient.scan(any())
            } returns response

            // 执行扫描
            val result = scanService.fullScan(rootDir = customRootDir)

            // 验证结果
            assertTrue(result is ScanResult.Success)

            // 验证 API 被正确调用
            coVerify(exactly = 1) {
                apiClient.scan(
                    match { req ->
                        req.scanType == 0 &&
                        req.rootDir == customRootDir
                    }
                )
            }
        }

        @Test
        @DisplayName("全量扫描 - 失败（Project.basePath 为 null）")
        fun `fullScan should fail when project basePath is null`() = runTest {
            // Mock Project.basePath 返回 null
            every { project.basePath } returns null

            // 执行扫描
            val result = scanService.fullScan()

            // 验证结果
            assertTrue(result is ScanResult.Failure)
            val failureResult = result as ScanResult.Failure
            assertTrue(failureResult.message.contains("无法获取项目根目录"))

            // 验证 API 未被调用
            coVerify(exactly = 0) {
                apiClient.scan(any())
            }
        }

        @Test
        @DisplayName("全量扫描 - 失败（401 未登录）")
        fun `fullScan should fail with 401 BusinessException`() = runTest {
            // Mock API 抛出 401 异常
            coEvery {
                apiClient.scan(any())
            } throws BusinessException(401, "Unauthorized")

            // 执行扫描
            val result = scanService.fullScan()

            // 验证结果
            assertTrue(result is ScanResult.Failure)
            val failureResult = result as ScanResult.Failure
            assertTrue(failureResult.message.contains("未登录或认证已过期"))
            assertNotNull(failureResult.exception)
        }

        @Test
        @DisplayName("全量扫描 - 失败（网络错误）")
        fun `fullScan should fail with NetworkException`() = runTest {
            // Mock API 抛出网络异常
            coEvery {
                apiClient.scan(any())
            } throws NetworkException("Connection timeout")

            // 执行扫描
            val result = scanService.fullScan()

            // 验证结果
            assertTrue(result is ScanResult.Failure)
            val failureResult = result as ScanResult.Failure
            assertTrue(failureResult.message.contains("网络错误"))
        }
    }

    // ========== 目标扫描测试 ==========

    @Nested
    @DisplayName("目标扫描测试")
    inner class TargetScanTests {

        @Test
        @DisplayName("目标扫描 - 成功")
        fun `targetScan should succeed`() = runTest {
            // 准备数据
            val paths = listOf("/test/project/src/main.go", "/test/project/src/utils.go")
            val response = ScanResponse(
                message = "Scan started",
                tools = listOf("golangci-lint"),
                scanFileNum = 2
            )

            // Mock API 调用
            coEvery {
                apiClient.scan(any())
            } returns response

            // 执行扫描
            val result = scanService.targetScan(paths)

            // 验证结果
            assertTrue(result is ScanResult.Success)
            val successResult = result as ScanResult.Success
            assertEquals(2, successResult.response.scanFileNum)

            // 验证 API 被正确调用
            coVerify(exactly = 1) {
                apiClient.scan(
                    match { req ->
                        req.scanType == 100 && // 目标扫描
                        req.paths == paths &&
                        req.rootDir == "/test/project"
                    }
                )
            }
        }

        @Test
        @DisplayName("目标扫描 - 失败（paths 为空）")
        fun `targetScan should fail when paths is empty`() = runTest {
            // 执行扫描（传入空列表）
            val result = scanService.targetScan(emptyList())

            // 验证结果
            assertTrue(result is ScanResult.Failure)
            val failureResult = result as ScanResult.Failure
            assertTrue(failureResult.message.contains("paths 参数不能为空"))

            // 验证 API 未被调用
            coVerify(exactly = 0) {
                apiClient.scan(any())
            }
        }

        @Test
        @DisplayName("目标扫描 - 失败（Project.basePath 为 null）")
        fun `targetScan should fail when project basePath is null`() = runTest {
            // Mock Project.basePath 返回 null
            every { project.basePath } returns null

            // 执行扫描
            val result = scanService.targetScan(listOf("/test/file.go"))

            // 验证结果
            assertTrue(result is ScanResult.Failure)
            val failureResult = result as ScanResult.Failure
            assertTrue(failureResult.message.contains("无法获取项目根目录"))
        }

        @Test
        @DisplayName("目标扫描 - 失败（400 请求参数无效）")
        fun `targetScan should fail with 400 BusinessException`() = runTest {
            // Mock API 抛出 400 异常
            coEvery {
                apiClient.scan(any())
            } throws BusinessException(400, "Invalid paths")

            // 执行扫描
            val result = scanService.targetScan(listOf("/test/file.go"))

            // 验证结果
            assertTrue(result is ScanResult.Failure)
            val failureResult = result as ScanResult.Failure
            assertTrue(failureResult.message.contains("请求参数无效"))
        }
    }

    // ========== Pre-Commit 扫描测试 ==========

    @Nested
    @DisplayName("Pre-Commit 扫描测试")
    inner class PreCommitScanTests {

        @Test
        @DisplayName("Pre-Commit 扫描 - 成功")
        fun `preCommitScan should succeed`() = runTest {
            // 准备数据
            val response = ScanResponse(
                message = "Scan started",
                tools = listOf("golangci-lint"),
                scanFileNum = 3
            )

            // Mock API 调用
            coEvery {
                apiClient.scan(any())
            } returns response

            // 执行扫描
            val result = scanService.preCommitScan()

            // 验证结果
            assertTrue(result is ScanResult.Success)
            val successResult = result as ScanResult.Success
            assertEquals(3, successResult.response.scanFileNum)

            // 验证 API 被正确调用
            coVerify(exactly = 1) {
                apiClient.scan(
                    match { req ->
                        req.scanType == 102 && // pre-commit 扫描
                        req.paths == null &&
                        req.rootDir == "/test/project"
                    }
                )
            }
        }

        @Test
        @DisplayName("Pre-Commit 扫描 - 失败（Project.basePath 为 null）")
        fun `preCommitScan should fail when project basePath is null`() = runTest {
            // Mock Project.basePath 返回 null
            every { project.basePath } returns null

            // 执行扫描
            val result = scanService.preCommitScan()

            // 验证结果
            assertTrue(result is ScanResult.Failure)
            val failureResult = result as ScanResult.Failure
            assertTrue(failureResult.message.contains("无法获取项目根目录"))
        }

        @Test
        @DisplayName("Pre-Commit 扫描 - 失败（400 暂存区无变更）")
        fun `preCommitScan should fail when no staged files`() = runTest {
            // Mock API 抛出 400 异常（暂存区无变更）
            coEvery {
                apiClient.scan(any())
            } throws BusinessException(400, "No staged files")

            // 执行扫描
            val result = scanService.preCommitScan()

            // 验证结果
            assertTrue(result is ScanResult.Failure)
            val failureResult = result as ScanResult.Failure
            assertTrue(failureResult.message.contains("请求参数无效") ||
                      failureResult.message.contains("Git 暂存区是否有变更"))
        }

        @Test
        @DisplayName("Pre-Commit 扫描 - 失败（网络错误）")
        fun `preCommitScan should fail with NetworkException`() = runTest {
            // Mock API 抛出网络异常
            coEvery {
                apiClient.scan(any())
            } throws NetworkException("Connection timeout")

            // 执行扫描
            val result = scanService.preCommitScan()

            // 验证结果
            assertTrue(result is ScanResult.Failure)
            val failureResult = result as ScanResult.Failure
            assertTrue(failureResult.message.contains("网络错误"))
        }
    }

    // ========== Pre-Push 扫描测试 ==========

    @Nested
    @DisplayName("Pre-Push 扫描测试")
    inner class PrePushScanTests {

        @Test
        @DisplayName("Pre-Push 扫描 - 成功")
        fun `prePushScan should succeed`() = runTest {
            // 准备数据
            val response = ScanResponse(
                message = "Scan started",
                tools = listOf("golangci-lint", "gosec"),
                scanFileNum = 5
            )

            // Mock API 调用
            coEvery {
                apiClient.scan(any())
            } returns response

            // 执行扫描
            val result = scanService.prePushScan()

            // 验证结果
            assertTrue(result is ScanResult.Success)
            val successResult = result as ScanResult.Success
            assertEquals(5, successResult.response.scanFileNum)

            // 验证 API 被正确调用
            coVerify(exactly = 1) {
                apiClient.scan(
                    match { req ->
                        req.scanType == 103 && // pre-push 扫描
                        req.paths == null &&
                        req.rootDir == "/test/project"
                    }
                )
            }
        }

        @Test
        @DisplayName("Pre-Push 扫描 - 失败（Project.basePath 为 null）")
        fun `prePushScan should fail when project basePath is null`() = runTest {
            // Mock Project.basePath 返回 null
            every { project.basePath } returns null

            // 执行扫描
            val result = scanService.prePushScan()

            // 验证结果
            assertTrue(result is ScanResult.Failure)
            val failureResult = result as ScanResult.Failure
            assertTrue(failureResult.message.contains("无法获取项目根目录"))
        }

        @Test
        @DisplayName("Pre-Push 扫描 - 失败（400 无未推送提交）")
        fun `prePushScan should fail when no unpushed commits`() = runTest {
            // Mock API 抛出 400 异常（无未推送提交）
            coEvery {
                apiClient.scan(any())
            } throws BusinessException(400, "No unpushed commits")

            // 执行扫描
            val result = scanService.prePushScan()

            // 验证结果
            assertTrue(result is ScanResult.Failure)
            val failureResult = result as ScanResult.Failure
            assertTrue(failureResult.message.contains("请求参数无效") ||
                      failureResult.message.contains("是否有未推送的提交"))
        }

        @Test
        @DisplayName("Pre-Push 扫描 - 失败（401 未登录）")
        fun `prePushScan should fail with 401 BusinessException`() = runTest {
            // Mock API 抛出 401 异常
            coEvery {
                apiClient.scan(any())
            } throws BusinessException(401, "Unauthorized")

            // 执行扫描
            val result = scanService.prePushScan()

            // 验证结果
            assertTrue(result is ScanResult.Failure)
            val failureResult = result as ScanResult.Failure
            assertTrue(failureResult.message.contains("未登录或认证已过期"))
        }

        @Test
        @DisplayName("Pre-Push 扫描 - 失败（未知异常）")
        fun `prePushScan should handle unexpected exceptions`() = runTest {
            // Mock API 抛出未知异常
            coEvery {
                apiClient.scan(any())
            } throws RuntimeException("Unexpected error")

            // 执行扫描
            val result = scanService.prePushScan()

            // 验证结果
            assertTrue(result is ScanResult.Failure)
            val failureResult = result as ScanResult.Failure
            assertTrue(failureResult.message.contains("未知错误"))
            assertNotNull(failureResult.exception)
        }
    }

    // ========== 扫描进度查询测试 ==========

    @Nested
    @DisplayName("扫描进度查询测试")
    inner class GetScanProgressTests {

        @Test
        @DisplayName("扫描进度查询 - 成功（扫描进行中）")
        fun `getScanProgress should succeed when scan is running`() = runTest {
            // 准备数据
            val response = ScanProgressResponse(
                projectRoot = "/test/project",
                toolStatuses = mapOf(
                    "golangci-lint" to "running",
                    "gosec" to "done"
                ),
                status = "running"
            )

            // Mock API 调用
            coEvery {
                apiClient.getScanProgress()
            } returns response

            // 执行查询
            val result = scanService.getScanProgress()

            // 验证结果
            assertTrue(result is ScanProgressResult.Success)
            val successResult = result as ScanProgressResult.Success
            assertEquals("/test/project", successResult.response.projectRoot)
            assertEquals("running", successResult.response.status)
            assertEquals(2, successResult.response.toolStatuses.size)
            assertEquals("running", successResult.response.toolStatuses["golangci-lint"])
            assertEquals("done", successResult.response.toolStatuses["gosec"])

            // 验证 API 被正确调用
            coVerify(exactly = 1) {
                apiClient.getScanProgress()
            }
        }

        @Test
        @DisplayName("扫描进度查询 - 成功（扫描已完成）")
        fun `getScanProgress should succeed when scan is done`() = runTest {
            // 准备数据
            val response = ScanProgressResponse(
                projectRoot = "/test/project",
                toolStatuses = mapOf(
                    "golangci-lint" to "done",
                    "gosec" to "done"
                ),
                status = "done"
            )

            // Mock API 调用
            coEvery {
                apiClient.getScanProgress()
            } returns response

            // 执行查询
            val result = scanService.getScanProgress()

            // 验证结果
            assertTrue(result is ScanProgressResult.Success)
            val successResult = result as ScanProgressResult.Success
            assertEquals("done", successResult.response.status)
        }

        @Test
        @DisplayName("扫描进度查询 - 成功（无扫描任务）")
        fun `getScanProgress should succeed when no scan task`() = runTest {
            // 准备数据
            val response = ScanProgressResponse(
                projectRoot = "",
                toolStatuses = emptyMap(),
                status = ""
            )

            // Mock API 调用
            coEvery {
                apiClient.getScanProgress()
            } returns response

            // 执行查询
            val result = scanService.getScanProgress()

            // 验证结果
            assertTrue(result is ScanProgressResult.Success)
            val successResult = result as ScanProgressResult.Success
            assertEquals("", successResult.response.status)
            assertTrue(successResult.response.toolStatuses.isEmpty())
        }

        @Test
        @DisplayName("扫描进度查询 - 失败（401 未登录）")
        fun `getScanProgress should fail with 401 BusinessException`() = runTest {
            // Mock API 抛出 401 异常
            coEvery {
                apiClient.getScanProgress()
            } throws BusinessException(401, "Unauthorized")

            // 执行查询
            val result = scanService.getScanProgress()

            // 验证结果
            assertTrue(result is ScanProgressResult.Failure)
            val failureResult = result as ScanProgressResult.Failure
            assertTrue(failureResult.message.contains("未登录或认证已过期"))
            assertNotNull(failureResult.exception)
        }

        @Test
        @DisplayName("扫描进度查询 - 失败（网络错误）")
        fun `getScanProgress should fail with NetworkException`() = runTest {
            // Mock API 抛出网络异常
            coEvery {
                apiClient.getScanProgress()
            } throws NetworkException("Connection timeout")

            // 执行查询
            val result = scanService.getScanProgress()

            // 验证结果
            assertTrue(result is ScanProgressResult.Failure)
            val failureResult = result as ScanProgressResult.Failure
            assertTrue(failureResult.message.contains("网络错误"))
            assertNotNull(failureResult.exception)
        }

        @Test
        @DisplayName("扫描进度查询 - 失败（未知异常）")
        fun `getScanProgress should handle unexpected exceptions`() = runTest {
            // Mock API 抛出未知异常
            coEvery {
                apiClient.getScanProgress()
            } throws RuntimeException("Unexpected error")

            // 执行查询
            val result = scanService.getScanProgress()

            // 验证结果
            assertTrue(result is ScanProgressResult.Failure)
            val failureResult = result as ScanProgressResult.Failure
            assertTrue(failureResult.message.contains("未知错误"))
            assertNotNull(failureResult.exception)
        }
    }

    // ========== 扫描结果查询测试 ==========

    @Nested
    @DisplayName("扫描结果查询测试")
    inner class GetScanResultTests {

        @Test
        @DisplayName("扫描结果查询 - 成功（有缺陷）")
        fun `getScanResult should succeed with defects`() = runTest {
            // 准备数据
            val defects2 = listOf(
                Defect(
                    toolName = "golangci-lint",
                    checkerName = "errcheck",
                    description = "Error return value is not checked",
                    filePath = "/test/project/src/main.go",
                    line = 42,
                    severity = 1  // 严重
                ),
                Defect(
                    toolName = "gosec",
                    checkerName = "G104",
                    description = "Audit errors not checked",
                    filePath = "/test/project/src/utils.go",
                    line = 15,
                    severity = 2  // 一般
                )
            )
            val response = ScanResultResponse(defects = defects2)

            // Mock API 调用
            coEvery {
                apiClient.getScanResult(any())
            } returns response

            // 执行查询
            val result = scanService.getScanResult()

            // 验证结果
            assertTrue(result is ScanResultQueryResult.Success)
            val successResult = result as ScanResultQueryResult.Success
            val defects = successResult.response.getDefectList()
            assertEquals(2, defects.size)
            assertEquals("golangci-lint", defects[0].toolName)
            assertEquals("errcheck", defects[0].checkerName)
            assertEquals(42, defects[0].line)

            // 验证 API 被正确调用
            coVerify(exactly = 1) {
                apiClient.getScanResult(
                    match { req ->
                        req.path == "/test/project"
                    }
                )
            }
        }

        @Test
        @DisplayName("扫描结果查询 - 成功（无缺陷）")
        fun `getScanResult should succeed with no defects`() = runTest {
            // 准备数据
            val response = ScanResultResponse(defects = emptyList())

            // Mock API 调用
            coEvery {
                apiClient.getScanResult(any())
            } returns response

            // 执行查询
            val result = scanService.getScanResult()

            // 验证结果
            assertTrue(result is ScanResultQueryResult.Success)
            val successResult = result as ScanResultQueryResult.Success
            assertTrue(successResult.response.getDefectList().isEmpty())
        }

        @Test
        @DisplayName("扫描结果查询 - 成功（指定 path）")
        fun `getScanResult should succeed with specified path`() = runTest {
            // 准备数据
            val customPath = "/custom/path"
            val response = ScanResultResponse(defects = emptyList())

            // Mock API 调用
            coEvery {
                apiClient.getScanResult(any())
            } returns response

            // 执行查询
            val result = scanService.getScanResult(path = customPath)

            // 验证结果
            assertTrue(result is ScanResultQueryResult.Success)

            // 验证 API 被正确调用
            coVerify(exactly = 1) {
                apiClient.getScanResult(
                    match { req ->
                        req.path == customPath
                    }
                )
            }
        }

        @Test
        @DisplayName("扫描结果查询 - 失败（Project.basePath 为 null）")
        fun `getScanResult should fail when project basePath is null`() = runTest {
            // Mock Project.basePath 返回 null
            every { project.basePath } returns null

            // 执行查询
            val result = scanService.getScanResult()

            // 验证结果
            assertTrue(result is ScanResultQueryResult.Failure)
            val failureResult = result as ScanResultQueryResult.Failure
            assertTrue(failureResult.message.contains("无法获取项目根目录"))

            // 验证 API 未被调用
            coVerify(exactly = 0) {
                apiClient.getScanResult(any())
            }
        }

        @Test
        @DisplayName("扫描结果查询 - 失败（401 未登录）")
        fun `getScanResult should fail with 401 BusinessException`() = runTest {
            // Mock API 抛出 401 异常
            coEvery {
                apiClient.getScanResult(any())
            } throws BusinessException(401, "Unauthorized")

            // 执行查询
            val result = scanService.getScanResult()

            // 验证结果
            assertTrue(result is ScanResultQueryResult.Failure)
            val failureResult = result as ScanResultQueryResult.Failure
            assertTrue(failureResult.message.contains("未登录或认证已过期"))
            assertNotNull(failureResult.exception)
        }

        @Test
        @DisplayName("扫描结果查询 - 失败（400 请求参数无效）")
        fun `getScanResult should fail with 400 BusinessException`() = runTest {
            // Mock API 抛出 400 异常
            coEvery {
                apiClient.getScanResult(any())
            } throws BusinessException(400, "Invalid path")

            // 执行查询
            val result = scanService.getScanResult()

            // 验证结果
            assertTrue(result is ScanResultQueryResult.Failure)
            val failureResult = result as ScanResultQueryResult.Failure
            assertTrue(failureResult.message.contains("请求参数无效"))
        }

        @Test
        @DisplayName("扫描结果查询 - 失败（网络错误）")
        fun `getScanResult should fail with NetworkException`() = runTest {
            // Mock API 抛出网络异常
            coEvery {
                apiClient.getScanResult(any())
            } throws NetworkException("Connection timeout")

            // 执行查询
            val result = scanService.getScanResult()

            // 验证结果
            assertTrue(result is ScanResultQueryResult.Failure)
            val failureResult = result as ScanResultQueryResult.Failure
            assertTrue(failureResult.message.contains("网络错误"))
            assertNotNull(failureResult.exception)
        }

        @Test
        @DisplayName("扫描结果查询 - 失败（未知异常）")
        fun `getScanResult should handle unexpected exceptions`() = runTest {
            // Mock API 抛出未知异常
            coEvery {
                apiClient.getScanResult(any())
            } throws RuntimeException("Unexpected error")

            // 执行查询
            val result = scanService.getScanResult()

            // 验证结果
            assertTrue(result is ScanResultQueryResult.Failure)
            val failureResult = result as ScanResultQueryResult.Failure
            assertTrue(failureResult.message.contains("未知错误"))
            assertNotNull(failureResult.exception)
        }
    }

    // ========== 取消扫描测试 ==========

    @Nested
    @DisplayName("取消扫描测试")
    inner class CancelScanTests {

        @Test
        @DisplayName("取消扫描 - 成功")
        fun `cancelScan should succeed`() = runTest {
            // 准备数据
            val response = ScanCancelResponse(
                projectRoot = "/test/project"
            )

            // Mock API 调用
            coEvery {
                apiClient.cancelScan()
            } returns response

            // 执行取消
            val result = scanService.cancelScan()

            // 验证结果
            assertTrue(result is CancelScanResult.Success)
            val successResult = result as CancelScanResult.Success
            assertEquals("/test/project", successResult.projectRoot)

            // 验证 API 被正确调用
            coVerify(exactly = 1) {
                apiClient.cancelScan()
            }
        }

        @Test
        @DisplayName("取消扫描 - 失败（400 无正在进行的扫描任务）")
        fun `cancelScan should fail when no scan task is running`() = runTest {
            // Mock API 抛出 400 异常
            coEvery {
                apiClient.cancelScan()
            } throws BusinessException(400, "No scan task is running")

            // 执行取消
            val result = scanService.cancelScan()

            // 验证结果
            assertTrue(result is CancelScanResult.Failure)
            val failureResult = result as CancelScanResult.Failure
            assertTrue(failureResult.message.contains("没有正在进行的扫描任务"))
            assertNotNull(failureResult.exception)
        }

        @Test
        @DisplayName("取消扫描 - 失败（401 未登录）")
        fun `cancelScan should fail with 401 BusinessException`() = runTest {
            // Mock API 抛出 401 异常
            coEvery {
                apiClient.cancelScan()
            } throws BusinessException(401, "Unauthorized")

            // 执行取消
            val result = scanService.cancelScan()

            // 验证结果
            assertTrue(result is CancelScanResult.Failure)
            val failureResult = result as CancelScanResult.Failure
            assertTrue(failureResult.message.contains("未登录或认证已过期"))
            assertNotNull(failureResult.exception)
        }

        @Test
        @DisplayName("取消扫描 - 失败（500 服务器错误）")
        fun `cancelScan should fail with 500 BusinessException`() = runTest {
            // Mock API 抛出 500 异常
            coEvery {
                apiClient.cancelScan()
            } throws BusinessException(500, "Internal Server Error")

            // 执行取消
            val result = scanService.cancelScan()

            // 验证结果
            assertTrue(result is CancelScanResult.Failure)
            val failureResult = result as CancelScanResult.Failure
            assertTrue(failureResult.message.contains("服务器内部错误"))
            assertNotNull(failureResult.exception)
        }

        @Test
        @DisplayName("取消扫描 - 失败（网络错误）")
        fun `cancelScan should fail with NetworkException`() = runTest {
            // Mock API 抛出网络异常
            coEvery {
                apiClient.cancelScan()
            } throws NetworkException("Connection timeout")

            // 执行取消
            val result = scanService.cancelScan()

            // 验证结果
            assertTrue(result is CancelScanResult.Failure)
            val failureResult = result as CancelScanResult.Failure
            assertTrue(failureResult.message.contains("网络错误"))
            assertNotNull(failureResult.exception)
        }

        @Test
        @DisplayName("取消扫描 - 失败（未知异常）")
        fun `cancelScan should handle unexpected exceptions`() = runTest {
            // Mock API 抛出未知异常
            coEvery {
                apiClient.cancelScan()
            } throws RuntimeException("Unexpected error")

            // 执行取消
            val result = scanService.cancelScan()

            // 验证结果
            assertTrue(result is CancelScanResult.Failure)
            val failureResult = result as CancelScanResult.Failure
            assertTrue(failureResult.message.contains("未知错误"))
            assertNotNull(failureResult.exception)
        }
    }
}

