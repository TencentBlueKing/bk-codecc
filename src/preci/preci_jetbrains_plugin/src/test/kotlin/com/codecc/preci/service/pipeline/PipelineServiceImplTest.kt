package com.codecc.preci.service.pipeline

import com.codecc.preci.BaseTest
import com.codecc.preci.core.http.BusinessException
import com.codecc.preci.core.http.NetworkException
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * PipelineServiceImpl 单元测试（MockWebServer 模拟 Local Server）
 *
 * @since 1.0
 */
@DisplayName("PipelineServiceImpl 测试")
class PipelineServiceImplTest : BaseTest() {

    private lateinit var mockServer: MockWebServer
    private lateinit var project: Project
    private lateinit var service: PipelineServiceImpl

    @BeforeEach
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()

        mockkObject(com.codecc.preci.util.PreCIPortDetector)
        every { com.codecc.preci.util.PreCIPortDetector.getServerPort() } returns mockServer.port

        project = mockk(relaxed = true)
        every { project.basePath } returns "/test/project"

        service = PipelineServiceImpl(project)
    }

    @AfterEach
    fun teardown() {
        try {
            mockServer.shutdown()
        } catch (_: Exception) {
            // 个别用例内已关闭 MockWebServer
        }
        unmockkAll()
    }

    @Test
    @DisplayName("获取构建历史 - 成功")
    fun `getBuildHistory should return Success with builds`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"builds":[{"buildId":"h1","buildNum":9,"startTime":1,"endTime":2,"status":"SUCCEED"}]}"""
                )
                .addHeader("Content-Type", "application/json")
        )

        val result = service.getBuildHistory()

        assertTrue(result is BuildHistoryResult.Success)
        val success = result as BuildHistoryResult.Success
        assertEquals(1, success.builds.size)
        assertEquals("h1", success.builds[0].buildId)

        val recorded = mockServer.takeRequest()
        assertEquals("GET", recorded.method)
        assertTrue(recorded.path!!.contains("/pipeline/build/history"))
        assertTrue(recorded.path!!.contains("rootPath="))
    }

    @Test
    @DisplayName("触发构建 - 成功")
    fun `startBuild should return Success with buildId`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"buildId":"started-99"}""")
                .addHeader("Content-Type", "application/json")
        )

        val result = service.startBuild()

        assertTrue(result is StartBuildResult.Success)
        assertEquals("started-99", (result as StartBuildResult.Success).buildId)

        val recorded = mockServer.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/pipeline/build/start", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("rootPath"))
    }

    @Test
    @DisplayName("获取构建日志 - 成功且包含日志行")
    fun `getBuildLogs should return Success with log lines`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                        "buildId":"lb1",
                        "finished":true,
                        "logs":[{"lineNo":1,"message":"done","timestamp":100}]
                    }
                    """.trimIndent()
                )
                .addHeader("Content-Type", "application/json")
        )

        val result = service.getBuildLogs("lb1", start = 0L)

        assertTrue(result is BuildLogsResult.Success)
        val ok = result as BuildLogsResult.Success
        assertEquals(1, ok.logs.size)
        assertEquals("done", ok.logs[0].message)
        assertTrue(ok.finished)

        val recorded = mockServer.takeRequest()
        assertTrue(recorded.path!!.contains("/pipeline/build/lb1/logs"))
        assertTrue(recorded.path!!.contains("start=0"))
    }

    @Test
    @DisplayName("停止构建 - 成功")
    fun `stopBuild should return Success`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .addHeader("Content-Type", "application/json")
        )

        val result = service.stopBuild("stop-id-1")

        assertTrue(result is StopBuildResult.Success)

        val recorded = mockServer.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertTrue(recorded.path!!.contains("/pipeline/build/stop-id-1/stop"))
    }

    @Test
    @DisplayName("获取构建历史 - 网络异常返回 Failure")
    fun `getBuildHistory should return Failure on network error`() {
        // 连接失败路径会触发拦截器内 runBlocking，避免使用 runTest 以免 UncompletedCoroutinesError
        mockServer.shutdown()

        val result = runBlocking { service.getBuildHistory() }

        assertTrue(result is BuildHistoryResult.Failure)
        val fail = result as BuildHistoryResult.Failure
        assertTrue(fail.message.contains("网络错误") || fail.message.contains("Network"))
        assertTrue(fail.cause is NetworkException)
    }

    @Test
    @DisplayName("触发构建 - 业务异常返回 Failure")
    fun `startBuild should return Failure on business error`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":"bad request"}""")
                .addHeader("Content-Type", "application/json")
        )

        val result = service.startBuild()

        assertTrue(result is StartBuildResult.Failure)
        val fail = result as StartBuildResult.Failure
        assertTrue(fail.message.isNotEmpty())
        assertTrue(fail.cause is BusinessException)
    }

    @Test
    @DisplayName("获取构建日志 - 业务异常返回 Failure")
    fun `getBuildLogs should return Failure on business error`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"error":"not found"}""")
                .addHeader("Content-Type", "application/json")
        )

        val result = service.getBuildLogs("missing", 0L)

        assertTrue(result is BuildLogsResult.Failure)
        val fail = result as BuildLogsResult.Failure
        assertTrue(fail.cause is BusinessException)
    }
}
