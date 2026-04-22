package com.codecc.preci.core.config

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Assertions.*

/**
 * PreCISettings 配置管理服务测试
 *
 * 测试配置管理服务的功能，包括配置的读取、修改和便捷方法
 *
 * 注意：由于 PreCISettings 依赖 IntelliJ Platform 的服务机制，
 * 这里主要测试配置访问器和便捷方法的逻辑
 *
 * @since 1.0
 */
class PreCISettingsTest : BasePlatformTestCase() {

    private lateinit var settings: PreCISettings

    override fun setUp() {
        super.setUp()
        settings = PreCISettings()
    }

    fun `test default configuration values`() {
        // Server 配置
        assertFalse(settings.serverAutoStart)
        assertEquals(30, settings.serverStartTimeout)

        // 扫描配置
        assertFalse(settings.scanOnSave)
        assertEquals(1, settings.defaultScanType)

        // 规则集配置
        assertTrue(settings.selectedCheckerSets.isEmpty())
        assertTrue(settings.rememberCheckerSets)

        // 结果展示配置
        assertTrue(settings.showSuccessNotification)
        assertTrue(settings.showErrorNotification)
        assertTrue(settings.autoOpenResults)

        // 高级配置
        assertFalse(settings.enableDebugLog)
        assertEquals(30, settings.requestTimeout)
        assertEquals(3, settings.maxRetries)
    }

    fun `test modify configuration`() {
        // 修改配置
        settings.serverAutoStart = true
        settings.serverStartTimeout = 60
        settings.scanOnSave = true
        settings.defaultScanType = 2
        settings.enableDebugLog = true

        // 验证修改
        assertTrue(settings.serverAutoStart)
        assertEquals(60, settings.serverStartTimeout)
        assertTrue(settings.scanOnSave)
        assertEquals(2, settings.defaultScanType)
        assertTrue(settings.enableDebugLog)
    }

    fun `test add checker set`() {
        // 添加规则集
        settings.addCheckerSet("set1")
        settings.addCheckerSet("set2")

        assertEquals(2, settings.selectedCheckerSets.size)
        assertTrue(settings.selectedCheckerSets.contains("set1"))
        assertTrue(settings.selectedCheckerSets.contains("set2"))
    }

    fun `test add duplicate checker set`() {
        // 添加重复的规则集
        settings.addCheckerSet("set1")
        settings.addCheckerSet("set1")

        // 应该只有一个
        assertEquals(1, settings.selectedCheckerSets.size)
        assertTrue(settings.selectedCheckerSets.contains("set1"))
    }

    fun `test remove checker set`() {
        // 添加规则集
        settings.addCheckerSet("set1")
        settings.addCheckerSet("set2")
        settings.addCheckerSet("set3")

        // 移除规则集
        settings.removeCheckerSet("set2")

        assertEquals(2, settings.selectedCheckerSets.size)
        assertTrue(settings.selectedCheckerSets.contains("set1"))
        assertFalse(settings.selectedCheckerSets.contains("set2"))
        assertTrue(settings.selectedCheckerSets.contains("set3"))
    }

    fun `test clear checker sets`() {
        // 添加规则集
        settings.addCheckerSet("set1")
        settings.addCheckerSet("set2")
        settings.addCheckerSet("set3")

        // 清空规则集
        settings.clearCheckerSets()

        assertTrue(settings.selectedCheckerSets.isEmpty())
    }

    fun `test reset to defaults`() {
        // 修改配置
        settings.serverAutoStart = true
        settings.requestTimeout = 60
        settings.maxRetries = 5
        settings.addCheckerSet("set1")

        // 重置为默认值
        settings.resetToDefaults()

        // 验证已重置
        assertFalse(settings.serverAutoStart)
        assertEquals(30, settings.requestTimeout)
        assertEquals(3, settings.maxRetries)
        assertTrue(settings.selectedCheckerSets.isEmpty())
    }

    fun `test get and load state`() {
        // 修改配置
        settings.serverAutoStart = true
        settings.requestTimeout = 60

        // 获取状态
        val state = settings.state

        assertNotNull(state)
        assertTrue(state.serverAutoStart)
        assertEquals(60, state.requestTimeout)

        // 创建新配置并加载状态
        val newSettings = PreCISettings()
        newSettings.loadState(state)

        assertTrue(newSettings.serverAutoStart)
        assertEquals(60, newSettings.requestTimeout)
    }

    fun `test configuration persistence`() {
        // 修改配置
        settings.serverAutoStart = true
        settings.serverStartTimeout = 45
        settings.scanOnSave = true
        settings.defaultScanType = 3
        settings.addCheckerSet("set1")
        settings.addCheckerSet("set2")

        // 获取状态
        val state = settings.state

        // 验证状态包含所有修改
        assertTrue(state.serverAutoStart)
        assertEquals(45, state.serverStartTimeout)
        assertTrue(state.scanOnSave)
        assertEquals(3, state.defaultScanType)
        assertEquals(2, state.selectedCheckerSets.size)
    }
}

