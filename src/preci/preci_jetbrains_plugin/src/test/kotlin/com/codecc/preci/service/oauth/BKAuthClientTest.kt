package com.codecc.preci.service.oauth

import com.codecc.preci.BaseTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("BKAuthClient 测试")
class BKAuthClientTest : BaseTest() {

    private lateinit var mockServer: MockWebServer

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
    }

    @AfterEach
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    @DisplayName("构造 authorize URL 包含所有必需参数")
    fun testBuildAuthorizeUrl() {
        val config = OAuthConfig(
            bkauthBaseUrl = mockServer.url("").toString().trimEnd('/'),
            clientId = "test-client",
            resource = "service:codecc",
            scope = "",
            redirectPath = "/api/preci/oauth/callback"
        )
        val url = BKAuthClient.buildAuthorizeUrl(
            config = config,
            codeChallenge = "test-challenge",
            state = "test-state",
            redirectUri = "http://127.0.0.1:63342/api/preci/oauth/callback"
        )

        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("client_id=test-client"))
        assertTrue(url.contains("code_challenge=test-challenge"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("state=test-state"))
        assertTrue(url.contains("resource="))
        assertTrue(url.contains("redirect_uri="))
    }

    @Test
    @DisplayName("code 换 token 成功")
    fun testExchangeCodeForTokenSuccess() {
        val tokenJson = """
            {
                "access_token": "bkci_test_access_token",
                "token_type": "Bearer",
                "expires_in": 7200,
                "refresh_token": "test_refresh_token",
                "scope": ""
            }
        """.trimIndent()
        mockServer.enqueue(MockResponse().setBody(tokenJson).setResponseCode(200))

        val config = OAuthConfig(
            bkauthBaseUrl = mockServer.url("").toString().trimEnd('/'),
            clientId = "test-client",
            resource = "service:codecc",
            scope = "",
            redirectPath = "/api/preci/oauth/callback"
        )

        val response = BKAuthClient.exchangeCodeForToken(
            config = config,
            code = "test-code",
            codeVerifier = "test-verifier",
            redirectUri = "http://127.0.0.1:63342/api/preci/oauth/callback"
        )

        assertEquals("bkci_test_access_token", response.accessToken)
        assertEquals(7200L, response.expiresIn)
        assertEquals("test_refresh_token", response.refreshToken)

        val request = mockServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.contains("/oauth2/token"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("grant_type=authorization_code"))
        assertTrue(body.contains("code=test-code"))
        assertTrue(body.contains("code_verifier=test-verifier"))
        assertTrue(body.contains("client_id=test-client"))
    }

    @Test
    @DisplayName("code 换 token 失败抛出异常")
    fun testExchangeCodeForTokenFailure() {
        mockServer.enqueue(MockResponse().setBody("""{"error":"invalid_grant"}""").setResponseCode(400))

        val config = OAuthConfig(
            bkauthBaseUrl = mockServer.url("").toString().trimEnd('/'),
            clientId = "test-client",
            resource = "service:codecc",
            scope = "",
            redirectPath = "/api/preci/oauth/callback"
        )

        val exception = assertThrows<OAuthException> {
            BKAuthClient.exchangeCodeForToken(
                config = config,
                code = "bad-code",
                codeVerifier = "test-verifier",
                redirectUri = "http://127.0.0.1:63342/api/preci/oauth/callback"
            )
        }
        assertTrue(exception.message!!.contains("invalid_grant") || exception.message!!.contains("400"))
    }
}
