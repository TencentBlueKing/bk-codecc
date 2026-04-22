package com.codecc.preci.core.http

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * PreCIErrorCode 单元测试
 *
 * 测试错误码常量和辅助方法
 *
 * @since 1.0
 */
class PreCIErrorCodeTest {

    @Test
    fun `isAuthError 应该正确识别认证错误`() {
        // Act & Assert
        assertTrue(PreCIErrorCode.isAuthError(PreCIErrorCode.CODE_INVALID_ACCESS_TOKEN))
        assertTrue(PreCIErrorCode.isAuthError(100005))
    }

    @Test
    fun `isAuthError 对于非认证错误应该返回 false`() {
        // Act & Assert
        assertFalse(PreCIErrorCode.isAuthError(PreCIErrorCode.CODE_INVALID_PROJECT_ID))
        assertFalse(PreCIErrorCode.isAuthError(100009))
        assertFalse(PreCIErrorCode.isAuthError(100000))
        assertFalse(PreCIErrorCode.isAuthError(0))
    }

    @Test
    fun `isProjectIdError 应该正确识别项目 ID 错误`() {
        // Act & Assert
        assertTrue(PreCIErrorCode.isProjectIdError(PreCIErrorCode.CODE_INVALID_PROJECT_ID))
        assertTrue(PreCIErrorCode.isProjectIdError(100009))
    }

    @Test
    fun `isProjectIdError 对于非项目 ID 错误应该返回 false`() {
        // Act & Assert
        assertFalse(PreCIErrorCode.isProjectIdError(PreCIErrorCode.CODE_INVALID_ACCESS_TOKEN))
        assertFalse(PreCIErrorCode.isProjectIdError(100005))
        assertFalse(PreCIErrorCode.isProjectIdError(100000))
        assertFalse(PreCIErrorCode.isProjectIdError(0))
    }

    @Test
    fun `错误码常量应该与 preci_server 保持一致`() {
        // Assert - 验证关键错误码的值
        assertEquals(100000, PreCIErrorCode.CODE_INVALID_ROOT_DIR)
        assertEquals(100001, PreCIErrorCode.CODE_INVALID_PATHS)
        assertEquals(100002, PreCIErrorCode.CODE_OTHER_SCAN_RUNNING)
        assertEquals(100003, PreCIErrorCode.CODE_NO_SCAN_TASK)
        assertEquals(100004, PreCIErrorCode.CODE_QUICK_LOGIN_ERROR)
        assertEquals(100005, PreCIErrorCode.CODE_INVALID_ACCESS_TOKEN)
        assertEquals(100006, PreCIErrorCode.CODE_NO_LATEST_VERSION)
        assertEquals(100007, PreCIErrorCode.CODE_IS_LATEST_VERSION)
        assertEquals(100008, PreCIErrorCode.CODE_DOWNLOAD_FAILED)
        assertEquals(100009, PreCIErrorCode.CODE_INVALID_PROJECT_ID)
        assertEquals(100010, PreCIErrorCode.CODE_INVALID_CHECKER_SET)
    }
}

