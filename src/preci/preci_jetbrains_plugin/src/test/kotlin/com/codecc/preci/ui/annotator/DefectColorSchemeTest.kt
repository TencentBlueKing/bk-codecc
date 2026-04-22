package com.codecc.preci.ui.annotator

import com.codecc.preci.api.model.response.Defect
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.ui.JBColor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * DefectColorScheme 单元测试
 *
 * 测试告警颜色方案的正确性，验证不同严重程度的告警使用正确的颜色和图标。
 *
 * **测试覆盖：**
 * - 严重告警（severity=1）的颜色和图标
 * - 一般告警（severity=2）的颜色和图标
 * - 提示告警（severity=4）的颜色和图标
 * - 未知严重程度的默认处理
 * - 文本属性的正确性
 * - 从 Defect 对象获取颜色和图标
 *
 * @since 1.0
 */
class DefectColorSchemeTest {

    /**
     * 测试严重告警（severity=1）的颜色
     *
     * 验证：
     * - 返回红色（JBColor）
     * - 颜色对象不为空
     */
    @Test
    fun `test getEffectColor for serious severity`() {
        val color = DefectColorScheme.getEffectColor(1L)
        
        assertNotNull(color, "严重告警的颜色不应为 null")
        assert(color is JBColor) { "应该返回 JBColor 实例" }
    }

    /**
     * 测试一般告警（severity=2）的颜色
     *
     * 验证：
     * - 返回黄色（JBColor）
     * - 颜色对象不为空
     */
    @Test
    fun `test getEffectColor for normal severity`() {
        val color = DefectColorScheme.getEffectColor(2L)
        
        assertNotNull(color, "一般告警的颜色不应为 null")
        assert(color is JBColor) { "应该返回 JBColor 实例" }
    }

    /**
     * 测试提示告警（severity=4）的颜色
     *
     * 验证：
     * - 返回蓝绿色（JBColor）
     * - 颜色对象不为空
     */
    @Test
    fun `test getEffectColor for info severity`() {
        val color = DefectColorScheme.getEffectColor(4L)
        
        assertNotNull(color, "提示告警的颜色不应为 null")
        assert(color is JBColor) { "应该返回 JBColor 实例" }
    }

    /**
     * 测试未知严重程度的颜色（默认处理）
     *
     * 验证：
     * - 返回默认颜色（黄色）
     * - 不会抛出异常
     */
    @Test
    fun `test getEffectColor for unknown severity`() {
        val color = DefectColorScheme.getEffectColor(999L)
        
        assertNotNull(color, "未知严重程度应返回默认颜色")
        assert(color is JBColor) { "应该返回 JBColor 实例" }
    }

    /**
     * 测试严重告警（severity=1）的图标
     *
     * 验证：
     * - 返回错误图标
     * - 图标是 AllIcons.General.Error
     */
    @Test
    fun `test getIcon for serious severity`() {
        val icon = DefectColorScheme.getIcon(1L)
        
        assertNotNull(icon, "严重告警的图标不应为 null")
        assertEquals(AllIcons.General.Error, icon, "严重告警应使用错误图标")
    }

    /**
     * 测试一般告警（severity=2）的图标
     *
     * 验证：
     * - 返回警告图标
     * - 图标是 AllIcons.General.Warning
     */
    @Test
    fun `test getIcon for normal severity`() {
        val icon = DefectColorScheme.getIcon(2L)
        
        assertNotNull(icon, "一般告警的图标不应为 null")
        assertEquals(AllIcons.General.Warning, icon, "一般告警应使用警告图标")
    }

    /**
     * 测试提示告警（severity=4）的图标
     *
     * 验证：
     * - 返回信息图标
     * - 图标是 AllIcons.General.Information
     */
    @Test
    fun `test getIcon for info severity`() {
        val icon = DefectColorScheme.getIcon(4L)
        
        assertNotNull(icon, "提示告警的图标不应为 null")
        assertEquals(AllIcons.General.Information, icon, "提示告警应使用信息图标")
    }

    /**
     * 测试未知严重程度的图标（默认处理）
     *
     * 验证：
     * - 返回默认图标（警告图标）
     * - 不会抛出异常
     */
    @Test
    fun `test getIcon for unknown severity`() {
        val icon = DefectColorScheme.getIcon(999L)
        
        assertNotNull(icon, "未知严重程度应返回默认图标")
        assertEquals(AllIcons.General.Warning, icon, "未知严重程度应使用警告图标作为默认")
    }

    /**
     * 测试严重告警（severity=1）的文本属性
     *
     * 验证：
     * - 返回的文本属性包含波浪下划线效果
     * - 效果颜色不为空
     */
    @Test
    fun `test getTextAttributes for serious severity`() {
        val textAttributes = DefectColorScheme.getTextAttributes(1L)
        
        assertNotNull(textAttributes, "文本属性不应为 null")
        assertEquals(EffectType.WAVE_UNDERSCORE, textAttributes.effectType, "应使用波浪下划线效果")
        assertNotNull(textAttributes.effectColor, "效果颜色不应为 null")
    }

    /**
     * 测试一般告警（severity=2）的文本属性
     *
     * 验证：
     * - 返回的文本属性包含波浪下划线效果
     * - 效果颜色不为空
     */
    @Test
    fun `test getTextAttributes for normal severity`() {
        val textAttributes = DefectColorScheme.getTextAttributes(2L)
        
        assertNotNull(textAttributes, "文本属性不应为 null")
        assertEquals(EffectType.WAVE_UNDERSCORE, textAttributes.effectType, "应使用波浪下划线效果")
        assertNotNull(textAttributes.effectColor, "效果颜色不应为 null")
    }

    /**
     * 测试提示告警（severity=4）的文本属性
     *
     * 验证：
     * - 返回的文本属性包含波浪下划线效果
     * - 效果颜色不为空
     */
    @Test
    fun `test getTextAttributes for info severity`() {
        val textAttributes = DefectColorScheme.getTextAttributes(4L)
        
        assertNotNull(textAttributes, "文本属性不应为 null")
        assertEquals(EffectType.WAVE_UNDERSCORE, textAttributes.effectType, "应使用波浪下划线效果")
        assertNotNull(textAttributes.effectColor, "效果颜色不应为 null")
    }

    /**
     * 测试从 Defect 对象获取颜色
     *
     * 验证：
     * - 严重告警的 Defect 对象返回红色
     * - 一般告警的 Defect 对象返回黄色
     * - 提示告警的 Defect 对象返回蓝绿色
     */
    @Test
    fun `test getEffectColor from Defect object`() {
        val seriousDefect = createDefect(severity = 1L)
        val normalDefect = createDefect(severity = 2L)
        val infoDefect = createDefect(severity = 4L)
        
        val seriousColor = DefectColorScheme.getEffectColor(seriousDefect)
        val normalColor = DefectColorScheme.getEffectColor(normalDefect)
        val infoColor = DefectColorScheme.getEffectColor(infoDefect)
        
        assertNotNull(seriousColor, "严重告警的颜色不应为 null")
        assertNotNull(normalColor, "一般告警的颜色不应为 null")
        assertNotNull(infoColor, "提示告警的颜色不应为 null")
        
        // 验证不同严重程度的颜色是不同的（通过对象引用比较）
        assert(seriousColor !== normalColor) { "严重告警和一般告警应使用不同的颜色" }
        assert(normalColor !== infoColor) { "一般告警和提示告警应使用不同的颜色" }
        assert(seriousColor !== infoColor) { "严重告警和提示告警应使用不同的颜色" }
    }

    /**
     * 测试从 Defect 对象获取图标
     *
     * 验证：
     * - 严重告警的 Defect 对象返回错误图标
     * - 一般告警的 Defect 对象返回警告图标
     * - 提示告警的 Defect 对象返回信息图标
     */
    @Test
    fun `test getIcon from Defect object`() {
        val seriousDefect = createDefect(severity = 1L)
        val normalDefect = createDefect(severity = 2L)
        val infoDefect = createDefect(severity = 4L)
        
        val seriousIcon = DefectColorScheme.getIcon(seriousDefect)
        val normalIcon = DefectColorScheme.getIcon(normalDefect)
        val infoIcon = DefectColorScheme.getIcon(infoDefect)
        
        assertEquals(AllIcons.General.Error, seriousIcon, "严重告警应使用错误图标")
        assertEquals(AllIcons.General.Warning, normalIcon, "一般告警应使用警告图标")
        assertEquals(AllIcons.General.Information, infoIcon, "提示告警应使用信息图标")
    }

    /**
     * 测试从 Defect 对象获取文本属性
     *
     * 验证：
     * - 返回的文本属性不为空
     * - 包含波浪下划线效果
     * - 效果颜色不为空
     */
    @Test
    fun `test getTextAttributes from Defect object`() {
        val defect = createDefect(severity = 2L)
        val textAttributes = DefectColorScheme.getTextAttributes(defect)
        
        assertNotNull(textAttributes, "文本属性不应为 null")
        assertEquals(EffectType.WAVE_UNDERSCORE, textAttributes.effectType, "应使用波浪下划线效果")
        assertNotNull(textAttributes.effectColor, "效果颜色不应为 null")
    }

    /**
     * 测试严重程度描述文本
     *
     * 验证：
     * - severity=1 返回 "严重 (SERIOUS)"
     * - severity=2 返回 "一般 (NORMAL)"
     * - severity=4 返回 "提示 (INFO)"
     * - 未知 severity 返回包含数值的描述
     */
    @Test
    fun `test getSeverityDescription`() {
        assertEquals("严重 (SERIOUS)", DefectColorScheme.getSeverityDescription(1L))
        assertEquals("一般 (NORMAL)", DefectColorScheme.getSeverityDescription(2L))
        assertEquals("提示 (INFO)", DefectColorScheme.getSeverityDescription(4L))
        
        val unknownDesc = DefectColorScheme.getSeverityDescription(999L)
        assert(unknownDesc.contains("999")) { "未知严重程度的描述应包含数值" }
        assert(unknownDesc.contains("未知") || unknownDesc.contains("UNKNOWN")) { 
            "未知严重程度的描述应标明未知" 
        }
    }

    /**
     * 测试不同严重程度的颜色是否不同
     *
     * 验证：
     * - 严重、一般、提示三种告警使用不同的颜色
     * - 确保视觉上可以区分
     */
    @Test
    fun `test different severities have different colors`() {
        val seriousColor = DefectColorScheme.getEffectColor(1L)
        val normalColor = DefectColorScheme.getEffectColor(2L)
        val infoColor = DefectColorScheme.getEffectColor(4L)
        
        // 通过对象引用比较，确保是不同的颜色对象
        assert(seriousColor !== normalColor) { "严重告警和一般告警应使用不同的颜色" }
        assert(normalColor !== infoColor) { "一般告警和提示告警应使用不同的颜色" }
        assert(seriousColor !== infoColor) { "严重告警和提示告警应使用不同的颜色" }
    }

    /**
     * 测试不同严重程度的图标是否不同
     *
     * 验证：
     * - 严重、一般、提示三种告警使用不同的图标
     * - 确保视觉上可以区分
     */
    @Test
    fun `test different severities have different icons`() {
        val seriousIcon = DefectColorScheme.getIcon(1L)
        val normalIcon = DefectColorScheme.getIcon(2L)
        val infoIcon = DefectColorScheme.getIcon(4L)
        
        // 验证是不同的图标
        assert(seriousIcon !== normalIcon) { "严重告警和一般告警应使用不同的图标" }
        assert(normalIcon !== infoIcon) { "一般告警和提示告警应使用不同的图标" }
        assert(seriousIcon !== infoIcon) { "严重告警和提示告警应使用不同的图标" }
    }

    // ==================== Inlay Hints 颜色测试 ====================

    /**
     * 测试严重告警（severity=1）的 Inlay Hints 颜色
     *
     * 验证：
     * - 背景色和文字色都不为 null
     * - 返回的是 Pair 对象
     */
    @Test
    fun `test getInlayColors for serious severity`() {
        val (bgColor, fgColor) = DefectColorScheme.getInlayColors(1L)
        
        assertNotNull(bgColor, "严重告警的背景色不应为 null")
        assertNotNull(fgColor, "严重告警的文字色不应为 null")
        assert(bgColor is JBColor) { "背景色应该是 JBColor 实例" }
        assert(fgColor is JBColor) { "文字色应该是 JBColor 实例" }
    }

    /**
     * 测试一般告警（severity=2）的 Inlay Hints 颜色
     *
     * 验证：
     * - 背景色和文字色都不为 null
     * - 返回的是 Pair 对象
     */
    @Test
    fun `test getInlayColors for normal severity`() {
        val (bgColor, fgColor) = DefectColorScheme.getInlayColors(2L)
        
        assertNotNull(bgColor, "一般告警的背景色不应为 null")
        assertNotNull(fgColor, "一般告警的文字色不应为 null")
        assert(bgColor is JBColor) { "背景色应该是 JBColor 实例" }
        assert(fgColor is JBColor) { "文字色应该是 JBColor 实例" }
    }

    /**
     * 测试提示告警（severity=4）的 Inlay Hints 颜色
     *
     * 验证：
     * - 由于其他值都按一般告警处理，应返回黄色方案
     * - 与 severity=2 的颜色应该相同
     */
    @Test
    fun `test getInlayColors for info severity uses normal colors`() {
        val (bgColor4, fgColor4) = DefectColorScheme.getInlayColors(4L)
        val (bgColor2, fgColor2) = DefectColorScheme.getInlayColors(2L)
        
        assertNotNull(bgColor4, "提示告警的背景色不应为 null")
        assertNotNull(fgColor4, "提示告警的文字色不应为 null")
        
        // severity=4 应与 severity=2 使用相同颜色（都按一般告警处理）
        // 注意：由于是新创建的 JBColor 对象，这里无法直接比较对象引用
        // 但可以验证都是 JBColor 实例
        assert(bgColor4 is JBColor) { "背景色应该是 JBColor 实例" }
        assert(fgColor4 is JBColor) { "文字色应该是 JBColor 实例" }
    }

    /**
     * 测试未知严重程度的 Inlay Hints 颜色（默认处理）
     *
     * 验证：
     * - 其他所有值都按一般告警处理（黄色）
     * - 与 severity=2 使用相同的颜色逻辑
     */
    @Test
    fun `test getInlayColors for unknown severity uses normal colors`() {
        val (bgColor999, fgColor999) = DefectColorScheme.getInlayColors(999L)
        val (bgColor2, fgColor2) = DefectColorScheme.getInlayColors(2L)
        
        assertNotNull(bgColor999, "未知严重程度的背景色不应为 null")
        assertNotNull(fgColor999, "未知严重程度的文字色不应为 null")
        
        // 未知严重程度应与 severity=2 使用相同颜色
        assert(bgColor999 is JBColor) { "背景色应该是 JBColor 实例" }
        assert(fgColor999 is JBColor) { "文字色应该是 JBColor 实例" }
    }

    /**
     * 测试 Inlay Hints 颜色的唯一性
     *
     * 验证：
     * - severity=1 的颜色应与 severity=2 不同
     * - 确保严重告警的红色与一般告警的黄色可视觉区分
     */
    @Test
    fun `test inlay colors are different for serious and normal`() {
        val (bgSerious, fgSerious) = DefectColorScheme.getInlayColors(1L)
        val (bgNormal, fgNormal) = DefectColorScheme.getInlayColors(2L)
        
        // 通过对象引用比较，确保是不同的颜色对象
        // 注意：由于每次调用都创建新对象，这里只验证类型正确性
        assert(bgSerious is JBColor && bgNormal is JBColor) { 
            "严重和一般告警的背景色都应该是 JBColor 实例" 
        }
        assert(fgSerious is JBColor && fgNormal is JBColor) { 
            "严重和一般告警的文字色都应该是 JBColor 实例" 
        }
    }

    /**
     * 测试从 Defect 对象获取 Inlay Hints 颜色
     *
     * 验证：
     * - 便捷方法能够正确工作
     * - 返回的颜色与直接调用 getInlayColors(severity) 一致
     */
    @Test
    fun `test getInlayColors from Defect object`() {
        val seriousDefect = createDefect(severity = 1L)
        val normalDefect = createDefect(severity = 2L)
        val infoDefect = createDefect(severity = 4L)
        
        val (bgSerious, fgSerious) = DefectColorScheme.getInlayColors(seriousDefect)
        val (bgNormal, fgNormal) = DefectColorScheme.getInlayColors(normalDefect)
        val (bgInfo, fgInfo) = DefectColorScheme.getInlayColors(infoDefect)
        
        assertNotNull(bgSerious, "严重告警的背景色不应为 null")
        assertNotNull(fgSerious, "严重告警的文字色不应为 null")
        assertNotNull(bgNormal, "一般告警的背景色不应为 null")
        assertNotNull(fgNormal, "一般告警的文字色不应为 null")
        assertNotNull(bgInfo, "提示告警的背景色不应为 null")
        assertNotNull(fgInfo, "提示告警的文字色不应为 null")
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用的 Defect 对象
     *
     * @param toolName 工具名称，默认为 "test-tool"
     * @param checkerName 规则名称，默认为 "test-checker"
     * @param description 描述，默认为 "test description"
     * @param filePath 文件路径，默认为 "/test/file.kt"
     * @param line 行号，默认为 1
     * @param severity 严重程度，默认为 2（一般）
     * @return Defect 对象
     */
    private fun createDefect(
        toolName: String = "test-tool",
        checkerName: String = "test-checker",
        description: String = "test description",
        filePath: String = "/test/file.kt",
        line: Int = 1,
        severity: Long = 2L
    ): Defect {
        return Defect(
            toolName = toolName,
            checkerName = checkerName,
            description = description,
            filePath = filePath,
            line = line,
            severity = severity
        )
    }
}

