package com.codecc.preci.api.client

import com.codecc.preci.BaseTest
import com.codecc.preci.api.model.request.StartBuildRequest
import com.codecc.preci.core.http.BusinessException
import com.codecc.preci.core.http.NetworkException
import io.mockk.every
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
import org.junit.jupiter.api.assertThrows

/**
 * PreCIApiClient 流水线相关 API 单元测试
 *
 * @since 1.0
 */
@DisplayName("PreCIApiClient 流水线 API 测试")
class PreCIApiClientPipelineTest : BaseTest() {

    private lateinit var mockServer: MockWebServer
    private lateinit var client: PreCIApiClient

    @BeforeEach
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()

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
    @DisplayName("获取构建历史 - 成功且 URL 含 rootPath、page、pageSize")
    fun `getPipelineBuildHistory should succeed and path contains query params`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"builds":[{"buildId":"b1","buildNum":1,"startTime":0,"endTime":0,"status":"RUNNING"}]}""")
                .addHeader("Content-Type", "application/json")
        )

        val root = "/proj/root"
        val response = client.getPipelineBuildHistory(root, page = 2, pageSize = 10)

        assertEquals(1, response.builds?.size)
        assertEquals("b1", response.builds?.get(0)?.buildId)

        val recorded = mockServer.takeRequest()
        assertEquals("GET", recorded.method)
        val path = recorded.path!!
        assertTrue(path.startsWith("/pipeline/build/history?"))
        assertTrue(path.contains("page=2"))
        assertTrue(path.contains("pageSize=10"))
        assertTrue(path.contains("rootPath="))
    }

    @Test
    @DisplayName("触发流水线构建 - 成功且 POST 体含 rootPath")
    fun `startPipelineBuild should succeed and post body contains rootPath`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"buildId":"new-1"}""")
                .addHeader("Content-Type", "application/json")
        )

        val req = StartBuildRequest(rootPath = "/workspace/app")
        val response = client.startPipelineBuild(req)

        assertEquals("new-1", response.buildId)

        val recorded = mockServer.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/pipeline/build/start", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("rootPath"))
        assertTrue(body.contains("/workspace/app"))
    }

    @Test
    @DisplayName("获取构建日志 - 成功且 URL 含 buildId、rootPath、start")
    fun `getPipelineBuildLogs should succeed and path contains buildId rootPath start`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"buildId":"bid","finished":false,"logs":[{"lineNo":1,"message":"hi","timestamp":1}]}"""
                )
                .addHeader("Content-Type", "application/json")
        )

        val response = client.getPipelineBuildLogs("/root/path", "build-abc", start = 10L)

        assertEquals("bid", response.buildId)
        assertEquals(1, response.logs?.size)

        val recorded = mockServer.takeRequest()
        assertEquals("GET", recorded.method)
        val path = recorded.path!!
        assertTrue(path.contains("/pipeline/build/build-abc/logs?"))
        assertTrue(path.contains("start=10"))
        assertTrue(path.contains("rootPath="))
    }

    @Test
    @DisplayName("停止流水线构建 - 成功且使用 DELETE")
    fun `stopPipelineBuild should succeed with DELETE`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .addHeader("Content-Type", "application/json")
        )

        val ok = client.stopPipelineBuild("/stop/root", "build-to-stop")

        assertEquals(true, ok)

        val recorded = mockServer.takeRequest()
        assertEquals("DELETE", recorded.method)
        val path = recorded.path!!
        assertTrue(path.contains("/pipeline/build/build-to-stop/stop?"))
        assertTrue(path.contains("rootPath="))
    }

    @Test
    @DisplayName("流水线接口 - 服务端返回业务错误")
    fun `pipeline call should throw BusinessException on error response`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("""{"error":"forbidden"}""")
                .addHeader("Content-Type", "application/json")
        )

        val ex = assertThrows<BusinessException> {
            client.getPipelineBuildHistory("/any", page = 1, pageSize = 20)
        }
        assertEquals(403, ex.httpCode)
    }

    @Test
    @DisplayName("流水线接口 - 网络连接失败")
    fun `pipeline call should throw NetworkException when server is down`() {
        // 连接失败会触发 ServerAutoStartInterceptor 内的 runBlocking，与 runTest 作用域冲突，故用 runBlocking 包裹挂起调用
        mockServer.shutdown()

        assertThrows<NetworkException> {
            runBlocking {
                client.startPipelineBuild(StartBuildRequest(rootPath = "/x"))
            }
        }
    }
}
