package com.codecc.preci.api.client

import com.codecc.preci.BaseTest
import com.codecc.preci.service.server.ServerManagementService
import com.codecc.preci.service.server.ServerStartResult
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import io.mockk.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.net.ConnectException
import java.util.concurrent.TimeUnit

/**
 * ServerAutoStartInterceptor 单元测试
 *
 * 核心测试场景：server 端口变更后，拦截器应使用新端口重建请求 URL 并成功重试。
 *
 * @since 1.0
 */
@DisplayName("ServerAutoStartInterceptor 测试")
class ServerAutoStartInterceptorTest : BaseTest() {

    private lateinit var newServer: MockWebServer
    private lateinit var mockProject: Project
    private lateinit var mockServerService: ServerManagementService

    @BeforeEach
    fun setup() {
        newServer = MockWebServer()
        newServer.start()

        mockProject = mockk(relaxed = true)
        mockServerService = mockk(relaxed = true)

        mockkStatic(ProjectManager::class)
        val mockProjectManager = mockk<ProjectManager>()
        every { ProjectManager.getInstance() } returns mockProjectManager
        every { mockProjectManager.defaultProject } returns mockProject

        mockkObject(ServerManagementService)
        every { ServerManagementService.getInstance(any()) } returns mockServerService
    }

    @AfterEach
    fun teardown() {
        newServer.shutdown()
        unmockkAll()
    }

    @Test
    @DisplayName("端口变更后应使用新端口重建请求 URL 并成功重试")
    fun `should rebuild request URL with new port after auto-start`() {
        val newPort = newServer.port

        coEvery { mockServerService.startServer() } returns ServerStartResult.Success(newPort)

        newServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"result":"ok"}""")
                .addHeader("Content-Type", "application/json")
        )

        val client = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .addInterceptor(ServerAutoStartInterceptor())
            .build()

        // 使用一个不可达的端口模拟旧端口连接失败
        val request = Request.Builder()
            .url("http://localhost:1/test-path")
            .get()
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
        assertEquals("""{"result":"ok"}""", response.body?.string())

        val recordedRequest = newServer.takeRequest()
        assertEquals("/test-path", recordedRequest.path)

        coVerify { mockServerService.startServer() }
    }

    @Test
    @DisplayName("自动启动失败时应抛出原始异常")
    fun `should throw original exception when auto-start fails`() {
        coEvery { mockServerService.startServer() } returns ServerStartResult.Failure("启动失败")

        val client = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .addInterceptor(ServerAutoStartInterceptor())
            .build()

        val request = Request.Builder()
            .url("http://localhost:1/test-path")
            .get()
            .build()

        assertThrows<ConnectException> {
            client.newCall(request).execute()
        }

        coVerify { mockServerService.startServer() }
    }

    @Test
    @DisplayName("正常请求不应触发自动启动")
    fun `should not trigger auto-start for successful requests`() {
        val server = MockWebServer()
        server.start()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"result":"ok"}""")
        )

        val client = OkHttpClient.Builder()
            .addInterceptor(ServerAutoStartInterceptor())
            .build()

        val request = Request.Builder()
            .url(server.url("/normal"))
            .get()
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
        coVerify(exactly = 0) { mockServerService.startServer() }

        server.shutdown()
    }
}
