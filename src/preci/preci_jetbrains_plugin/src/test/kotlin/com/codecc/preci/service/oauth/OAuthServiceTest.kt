package com.codecc.preci.service.oauth

import com.codecc.preci.BaseTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OAuthService 测试")
class OAuthServiceTest : BaseTest() {

    private lateinit var service: OAuthService

    @BeforeEach
    fun setUp() {
        service = OAuthService()
    }

    @Test
    @DisplayName("handleCallback 在有活跃会话时正确传递 code")
    fun testHandleCallbackSuccess() = runTest {
        val state = service.createSession()
        val result = CompletableDeferred<String>()

        launch {
            val code = service.awaitAuthorizationCode(state, timeoutMs = 5000L)
            result.complete(code)
        }

        delay(100)
        val handled = service.handleCallback(code = "test-code-123", state = state)
        assertTrue(handled)
        assertEquals("test-code-123", result.await())
    }

    @Test
    @DisplayName("handleCallback 在 state 不匹配时返回 false")
    fun testHandleCallbackStateMismatch() {
        service.createSession()
        val handled = service.handleCallback(code = "test-code", state = "wrong-state")
        assertFalse(handled)
    }

    @Test
    @DisplayName("handleCallback 在无活跃会话时返回 false")
    fun testHandleCallbackNoSession() {
        val handled = service.handleCallback(code = "test-code", state = "any-state")
        assertFalse(handled)
    }

    @Test
    @DisplayName("createSession 返回不同的 state")
    fun testCreateSessionUniqueness() {
        val s1 = service.createSession()
        service.cancelSession()
        val s2 = service.createSession()
        assertNotEquals(s1, s2)
    }

    @Test
    @DisplayName("cancelSession 使等待中的 awaitAuthorizationCode 抛出异常")
    fun testCancelSession() = runTest {
        val state = service.createSession()
        val exceptionRef = CompletableDeferred<Throwable?>()

        launch {
            try {
                service.awaitAuthorizationCode(state, timeoutMs = 60_000L)
                exceptionRef.complete(null)
            } catch (e: Throwable) {
                exceptionRef.complete(e)
            }
        }

        delay(50)
        service.cancelSession()

        val exception = exceptionRef.await()
        assertNotNull(exception)
        assertTrue(exception is CancellationException)
    }

    @Test
    @DisplayName("getSessionCodeVerifier 返回当前会话的 code_verifier")
    fun testGetSessionCodeVerifier() {
        val state = service.createSession()
        val verifier = service.getSessionCodeVerifier(state)
        assertNotNull(verifier)
        assertTrue(verifier!!.length in 43..128)
    }
}
