package com.codecc.preci.core.http

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * BusinessException 扩展功能单元测试
 *
 * 测试 BusinessException 的错误码识别功能
 *
 * @since 1.0
 */
class BusinessExceptionTest {

    @Test
    fun `BusinessException 应该包含 HTTP 状态码和错误码`() {
        // Arrange
        val exception = BusinessException(
            httpCode = 401,
            message = "[100005] access token 无效, 请重新登录",
            errorCode = 100005
        )

        // Assert
        assertEquals(401, exception.httpCode)
        assertEquals("[100005] access token 无效, 请重新登录", exception.message)
        assertEquals(100005, exception.errorCode)
    }

    @Test
    fun `isAuthError 应该正确识别认证错误`() {
        // Arrange
        val authException = BusinessException(
            httpCode = 401,
            message = "[100005] access token 无效, 请重新登录",
            errorCode = PreCIErrorCode.CODE_INVALID_ACCESS_TOKEN
        )

        // Act & Assert
        assertTrue(authException.isAuthError())
    }

    @Test
    fun `isAuthError 对于非认证错误应该返回 false`() {
        // Arrange
        val projectIdException = BusinessException(
            httpCode = 400,
            message = "[100009] 蓝盾项目 id 无效或缺失",
            errorCode = PreCIErrorCode.CODE_INVALID_PROJECT_ID
        )

        val normalException = BusinessException(
            httpCode = 400,
            message = "普通错误",
            errorCode = null
        )

        // Act & Assert
        assertFalse(projectIdException.isAuthError())
        assertFalse(normalException.isAuthError())
    }

    @Test
    fun `isProjectIdError 应该正确识别项目 ID 错误`() {
        // Arrange
        val projectIdException = BusinessException(
            httpCode = 400,
            message = "[100009] 蓝盾项目 id 无效或缺失",
            errorCode = PreCIErrorCode.CODE_INVALID_PROJECT_ID
        )

        // Act & Assert
        assertTrue(projectIdException.isProjectIdError())
    }

    @Test
    fun `isProjectIdError 对于非项目 ID 错误应该返回 false`() {
        // Arrange
        val authException = BusinessException(
            httpCode = 401,
            message = "[100005] access token 无效, 请重新登录",
            errorCode = PreCIErrorCode.CODE_INVALID_ACCESS_TOKEN
        )

        val normalException = BusinessException(
            httpCode = 400,
            message = "普通错误",
            errorCode = null
        )

        // Act & Assert
        assertFalse(authException.isProjectIdError())
        assertFalse(normalException.isProjectIdError())
    }

    @Test
    fun `errorCode 可以为 null`() {
        // Arrange
        val exception = BusinessException(
            httpCode = 400,
            message = "普通错误"
        )

        // Assert
        assertNull(exception.errorCode)
        assertFalse(exception.isAuthError())
        assertFalse(exception.isProjectIdError())
    }

    @Test
    fun `异常应该可以正常抛出和捕获`() {
        // Arrange
        val exception = BusinessException(
            httpCode = 401,
            message = "[100005] access token 无效, 请重新登录",
            errorCode = PreCIErrorCode.CODE_INVALID_ACCESS_TOKEN
        )

        // Act & Assert
        assertThrows(BusinessException::class.java) {
            throw exception
        }
    }

    @Test
    fun `异常消息应该正确传递`() {
        // Arrange
        val message = "[100005] access token 无效, 请重新登录"
        val exception = BusinessException(
            httpCode = 401,
            message = message,
            errorCode = 100005
        )

        // Act & Assert
        assertEquals(message, exception.message)
    }
}

