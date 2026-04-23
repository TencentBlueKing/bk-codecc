package com.codecc.preci.api.model.response

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * ErrorResponse 单元测试
 *
 * 测试错误响应的错误码提取和错误消息提取功能
 *
 * @since 1.0
 */
class ErrorResponseTest {

    @Test
    fun `extractErrorCode 应该正确提取错误码`() {
        // Arrange
        val errorResponse = ErrorResponse("[100005] access token 无效, 请重新登录")

        // Act
        val errorCode = errorResponse.extractErrorCode()

        // Assert
        assertEquals(100005, errorCode)
    }

    @Test
    fun `extractErrorCode 对于无错误码的消息应该返回 null`() {
        // Arrange
        val errorResponse = ErrorResponse("普通错误消息")

        // Act
        val errorCode = errorResponse.extractErrorCode()

        // Assert
        assertNull(errorCode)
    }

    @Test
    fun `extractErrorCode 对于格式错误的消息应该返回 null`() {
        // Arrange
        val errorResponse = ErrorResponse("[abc] 格式错误的消息")

        // Act
        val errorCode = errorResponse.extractErrorCode()

        // Assert
        assertNull(errorCode)
    }

    @Test
    fun `extractErrorMessage 应该正确提取纯错误消息`() {
        // Arrange
        val errorResponse = ErrorResponse("[100005] access token 无效, 请重新登录")

        // Act
        val message = errorResponse.extractErrorMessage()

        // Assert
        assertEquals("access token 无效, 请重新登录", message)
    }

    @Test
    fun `extractErrorMessage 对于无错误码的消息应该返回原消息`() {
        // Arrange
        val errorResponse = ErrorResponse("普通错误消息")

        // Act
        val message = errorResponse.extractErrorMessage()

        // Assert
        assertEquals("普通错误消息", message)
    }

    @Test
    fun `extractErrorMessage 应该去除首尾空格`() {
        // Arrange
        val errorResponse = ErrorResponse("[100005]   access token 无效, 请重新登录   ")

        // Act
        val message = errorResponse.extractErrorMessage()

        // Assert
        assertEquals("access token 无效, 请重新登录", message.trim())
    }

    @Test
    fun `extractErrorCode 应该支持多种错误码`() {
        // Arrange & Act & Assert
        assertEquals(100009, ErrorResponse("[100009] 蓝盾项目 id 无效或缺失").extractErrorCode())
        assertEquals(100000, ErrorResponse("[100000] 项目根目录不合法").extractErrorCode())
        assertEquals(100010, ErrorResponse("[100010] 扫描规则集无效或缺失").extractErrorCode())
    }

    @Test
    fun `extractErrorMessage 应该支持多行消息`() {
        // Arrange
        val multilineMessage = """[100010] 扫描规则集无效或缺失。
	1. 用 preci checkerset list 查看可用规则集；
	2. 用 preci checkerset select <规则集id> 指定规则集；
	3. 用 preci init 重新初始化。
然后再尝试重新扫描。"""
        val errorResponse = ErrorResponse(multilineMessage)

        // Act
        val message = errorResponse.extractErrorMessage()

        // Assert
        assertTrue(message.startsWith("扫描规则集无效或缺失"))
        assertTrue(message.contains("preci checkerset list"))
    }
}

