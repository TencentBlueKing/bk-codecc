package com.codecc.preci.api.client

import com.codecc.preci.BaseTest
import com.codecc.preci.api.model.request.CheckerSetSelectRequest
import com.codecc.preci.api.model.request.InitRequest
import com.codecc.preci.api.model.request.LoginRequest
import com.codecc.preci.api.model.request.ScanRequest
import com.codecc.preci.api.model.request.ScanResultRequest
import com.codecc.preci.core.http.BusinessException
import com.codecc.preci.core.http.NetworkException
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * PreCIApiClient 单元测试
 *
 * 使用 MockWebServer 模拟 HTTP 服务端，测试 API 客户端的各种场景
 *
 * @since 1.0
 */
@DisplayName("PreCIApiClient 测试")
class PreCIApiClientTest : BaseTest() {

    private lateinit var mockServer: MockWebServer
    private lateinit var client: PreCIApiClient

    @BeforeEach
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()

        // Mock PreCIPortDetector.getServerPort() 返回 mockServer 的端口
        mockkObject(com.codecc.preci.util.PreCIPortDetector)
        every { com.codecc.preci.util.PreCIPortDetector.getServerPort() } returns mockServer.port

        client = PreCIApiClient()
    }

    @AfterEach
    fun teardown() {
        mockServer.shutdown()
        unmockkAll()
    }

    @Test
    @DisplayName("登录 - 成功")
    fun `login should return LoginResponse on success`() = runTest {
        // 模拟成功响应
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"projectId":"proj123","userId":"user456"}""")
                .addHeader("Content-Type", "application/json")
        )

        val request = LoginRequest(pinToken = "123:abc", projectId = "proj123")
        val response = client.login(request)

        assertEquals("proj123", response.projectId)
        assertEquals("user456", response.userId)

        // 验证请求
        val recordedRequest = mockServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/auth/login", recordedRequest.path)
    }

    @Test
    @DisplayName("登录 - 业务异常")
    fun `login should throw BusinessException on 401`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":"Authentication failed"}""")
                .addHeader("Content-Type", "application/json")
        )

        val request = LoginRequest(pinToken = "123:abc")

        val exception = assertThrows<BusinessException> {
            client.login(request)
        }

        assertEquals(401, exception.httpCode)
        assertEquals("Authentication failed", exception.message)
    }

    @Test
    @DisplayName("项目初始化 - 成功")
    fun `initProject should return InitResponse on success`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"rootPath":"/project/root"}""")
                .addHeader("Content-Type", "application/json")
        )

        val request = InitRequest(currentPath = "/project/src")
        val response = client.initProject(request)

        assertEquals("/project/root", response.rootPath)
    }

    @Test
    @DisplayName("代码扫描 - 成功")
    fun `scan should return ScanResponse on success`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"message":"Scan started","tools":["tool1","tool2"],"scanFileNum":100}""")
                .addHeader("Content-Type", "application/json")
        )

        val request = ScanRequest(scanType = 1)
        val response = client.scan(request)

        assertEquals("Scan started", response.message)
        assertEquals(2, response.tools.size)
        assertEquals(100, response.scanFileNum)
    }

    @Test
    @DisplayName("查询扫描进度 - 成功")
    fun `getScanProgress should return ScanProgressResponse on success`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"projectRoot":"/root","toolStatuses":{"tool1":"running"},"status":"running"}""")
                .addHeader("Content-Type", "application/json")
        )

        val response = client.getScanProgress()

        assertEquals("/root", response.projectRoot)
        assertEquals("running", response.status)
    }

    @Test
    @DisplayName("查询扫描结果 - 成功")
    fun `getScanResult should return ScanResultResponse on success`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "defects": [
                            {
                                "toolName": "tool1",
                                "checkerName": "check1",
                                "description": "Issue found",
                                "filePath": "/file.go",
                                "line": 10
                            }
                        ]
                    }
                """.trimIndent())
                .addHeader("Content-Type", "application/json")
        )

        val request = ScanResultRequest(path = "/root")
        val response = client.getScanResult(request)

        val defects = response.getDefectList()
        assertEquals(1, defects.size)
        val defect = defects[0]
        assertEquals("tool1", defect.toolName)
        assertEquals(10, defect.line)
    }

    @Test
    @DisplayName("取消扫描 - 成功")
    fun `cancelScan should return ScanCancelResponse on success`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"projectRoot":"/root"}""")
                .addHeader("Content-Type", "application/json")
        )

        val response = client.cancelScan()

        assertEquals("/root", response.projectRoot)
    }

    @Test
    @DisplayName("获取规则集列表 - 成功")
    fun `getCheckerSetList should return CheckerSetListResponse on success`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "checkerSets": [
                            {
                                "checkerSetId": "set1",
                                "checkerSetName": "Standard",
                                "toolName": "tool1"
                            }
                        ]
                    }
                """.trimIndent())
                .addHeader("Content-Type", "application/json")
        )

        val response = client.getCheckerSetList()

        assertEquals(1, response.checkerSets.size)
        assertEquals("set1", response.checkerSets[0].checkerSetId)
    }

    @Test
    @DisplayName("选择规则集 - 成功")
    fun `selectCheckerSet should return CheckerSetSelectResponse on success`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"projectRoot":"/root","checkerSets":["set1","set2"]}""")
                .addHeader("Content-Type", "application/json")
        )

        val request = CheckerSetSelectRequest(checkerSets = listOf("set1", "set2"))
        val response = client.selectCheckerSet(request)

        assertEquals("/root", response.projectRoot)
        assertEquals(2, response.checkerSets.size)
    }

    @Test
    @DisplayName("停止服务 - 成功")
    fun `shutdown should complete without error`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .addHeader("Content-Type", "application/json")
        )

        // 调用 shutdown 方法
        client.shutdown()

        // 验证请求
        val recordedRequest = mockServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertEquals("/shutdown", recordedRequest.path)
    }

    @Test
    @DisplayName("获取最新版本 - 成功")
    fun `getLatestVersion should return LatestVersionResponse on success`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"latestVersion":"v2.1.0"}""")
                .addHeader("Content-Type", "application/json")
        )

        val response = client.getLatestVersion()

        assertEquals("v2.1.0", response.latestVersion)
    }

    @Test
    @DisplayName("下载最新版本 - 成功")
    fun `downloadLatest should complete without error`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .addHeader("Content-Type", "application/json")
        )

        // 调用 downloadLatest 方法
        client.downloadLatest()

        // 验证请求
        val recordedRequest = mockServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertEquals("/misc/downloadLatest", recordedRequest.path)
    }

    @Test
    @DisplayName("处理 HTTP 500 错误")
    fun `should throw BusinessException on HTTP 500`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error":"Internal server error"}""")
                .addHeader("Content-Type", "application/json")
        )

        val request = ScanRequest(scanType = 1)

        val exception = assertThrows<BusinessException> {
            client.scan(request)
        }

        assertEquals(500, exception.httpCode)
    }

    @Test
    @DisplayName("处理空响应体的错误")
    fun `should handle empty error response body`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("")
        )

        val request = ScanRequest(scanType = 1)

        val exception = assertThrows<BusinessException> {
            client.scan(request)
        }

        assertEquals(404, exception.httpCode)
    }

    @Test
    @DisplayName("网络连接失败")
    fun `should throw NetworkException on connection failure`() = runTest {
        // 关闭服务器模拟连接失败
        mockServer.shutdown()

        val request = ScanRequest(scanType = 1)

        assertThrows<NetworkException> {
            client.scan(request)
        }
    }

}

