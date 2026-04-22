package com.codecc.preci.core.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * PreCISettingsState 数据类测试
 *
 * 测试配置状态数据类的创建、默认值和修改功能
 *
 * @since 1.0
 */
@DisplayName("PreCISettingsState 测试")
class PreCISettingsStateTest {

    @Test
    @DisplayName("创建默认配置状态")
    fun `should create default settings state`() {
        val state = PreCISettingsState()

        // Local Server 配置
        assertFalse(state.serverAutoStart)
        assertEquals(30, state.serverStartTimeout)

        // 扫描配置
        assertFalse(state.scanOnSave)
        assertEquals(1, state.defaultScanType)

        // 规则集配置
        assertTrue(state.selectedCheckerSets.isEmpty())
        assertTrue(state.rememberCheckerSets)

        // 结果展示配置
        assertTrue(state.showSuccessNotification)
        assertTrue(state.showErrorNotification)
        assertTrue(state.autoOpenResults)

        // 高级配置
        assertFalse(state.enableDebugLog)
        assertEquals(30, state.requestTimeout)
        assertEquals(3, state.maxRetries)
    }

    @Test
    @DisplayName("修改配置状态")
    fun `should modify settings state`() {
        val state = PreCISettingsState()

        // 修改配置
        state.serverAutoStart = true
        state.serverStartTimeout = 60
        state.scanOnSave = true
        state.defaultScanType = 2
        state.rememberCheckerSets = false
        state.showSuccessNotification = false
        state.enableDebugLog = true
        state.requestTimeout = 60
        state.maxRetries = 5

        // 验证修改
        assertTrue(state.serverAutoStart)
        assertEquals(60, state.serverStartTimeout)
        assertTrue(state.scanOnSave)
        assertEquals(2, state.defaultScanType)
        assertFalse(state.rememberCheckerSets)
        assertFalse(state.showSuccessNotification)
        assertTrue(state.enableDebugLog)
        assertEquals(60, state.requestTimeout)
        assertEquals(5, state.maxRetries)
    }

    @Test
    @DisplayName("操作规则集列表")
    fun `should handle checker sets list`() {
        val state = PreCISettingsState()

        // 添加规则集
        state.selectedCheckerSets.add("set1")
        state.selectedCheckerSets.add("set2")
        state.selectedCheckerSets.add("set3")

        assertEquals(3, state.selectedCheckerSets.size)
        assertTrue(state.selectedCheckerSets.contains("set1"))
        assertTrue(state.selectedCheckerSets.contains("set2"))
        assertTrue(state.selectedCheckerSets.contains("set3"))

        // 移除规则集
        state.selectedCheckerSets.remove("set2")

        assertEquals(2, state.selectedCheckerSets.size)
        assertFalse(state.selectedCheckerSets.contains("set2"))

        // 清空规则集
        state.selectedCheckerSets.clear()

        assertTrue(state.selectedCheckerSets.isEmpty())
    }

    @Test
    @DisplayName("复制配置状态")
    fun `should copy settings state`() {
        val original = PreCISettingsState(
            serverAutoStart = true,
            serverStartTimeout = 45,
            scanOnSave = true,
            defaultScanType = 3
        )

        val copy = original.copy()

        assertEquals(original.serverAutoStart, copy.serverAutoStart)
        assertEquals(original.serverStartTimeout, copy.serverStartTimeout)
        assertEquals(original.scanOnSave, copy.scanOnSave)
        assertEquals(original.defaultScanType, copy.defaultScanType)
    }

    @Test
    @DisplayName("复制并修改配置状态")
    fun `should copy and modify settings state`() {
        val original = PreCISettingsState(
            serverAutoStart = false,
            requestTimeout = 30
        )

        val modified = original.copy(
            serverAutoStart = true,
            requestTimeout = 60
        )

        // 原始对象不变
        assertFalse(original.serverAutoStart)
        assertEquals(30, original.requestTimeout)

        // 修改后的对象改变
        assertTrue(modified.serverAutoStart)
        assertEquals(60, modified.requestTimeout)
    }

    @Test
    @DisplayName("数据类相等性")
    fun `should check equality`() {
        val state1 = PreCISettingsState(
            serverAutoStart = true,
            requestTimeout = 30
        )

        val state2 = PreCISettingsState(
            serverAutoStart = true,
            requestTimeout = 30
        )

        val state3 = PreCISettingsState(
            serverAutoStart = false,
            requestTimeout = 30
        )

        assertEquals(state1, state2)
        assertNotEquals(state1, state3)
    }
}

