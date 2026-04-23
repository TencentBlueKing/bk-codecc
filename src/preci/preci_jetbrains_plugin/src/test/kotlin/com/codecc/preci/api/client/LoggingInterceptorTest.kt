package com.codecc.preci.api.client

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * LoggingInterceptor 单元测试
 *
 * 测试日志拦截器的功能
 *
 * @since 1.0
 */
@DisplayName("LoggingInterceptor 测试")
class LoggingInterceptorTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var client: OkHttpClient

    @BeforeEach
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()

        client = OkHttpClient.Builder()
            .addInterceptor(LoggingInterceptor())
            .build()
    }

    @AfterEach
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    @DisplayName("应该成功拦截并记录请求和响应")
    fun `should intercept and log request and response`() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"result":"success"}""")
                .addHeader("Content-Type", "application/json")
        )

        val request = Request.Builder()
            .url(mockServer.url("/test"))
            .get()
            .build()

        val response = client.newCall(request).execute()

        assertTrue(response.isSuccessful)
        assertEquals(200, response.code)
        assertEquals("""{"result":"success"}""", response.body?.string())
    }

    @Test
    @DisplayName("应该正确处理 POST 请求")
    fun `should handle POST request with body`() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody("""{"id":"123"}""")
        )

        val mediaType = "application/json".toMediaType()
        val requestBody = """{"name":"test"}""".toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(mockServer.url("/create"))
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()

        assertEquals(201, response.code)
    }

    @Test
    @DisplayName("应该记录失败的请求")
    fun `should log failed requests`() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error":"Internal Server Error"}""")
        )

        val request = Request.Builder()
            .url(mockServer.url("/error"))
            .get()
            .build()

        val response = client.newCall(request).execute()

        assertFalse(response.isSuccessful)
        assertEquals(500, response.code)
    }

    @Test
    @DisplayName("应该处理空响应体")
    fun `should handle empty response body`() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(204)
                .setBody("")
        )

        val request = Request.Builder()
            .url(mockServer.url("/no-content"))
            .get()
            .build()

        val response = client.newCall(request).execute()

        assertEquals(204, response.code)
        assertEquals("", response.body?.string())
    }
}

