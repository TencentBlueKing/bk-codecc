package com.codecc.preci.ui.annotator

import com.codecc.preci.api.model.response.Defect
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * PreCIDefectBlockRenderer 单元测试
 *
 * 测试多行描述的状态管理逻辑，包括展开/折叠状态切换、行数计算等。
 *
 * **测试覆盖：**
 * - 单行描述的 isClickable 返回 false
 * - 多行描述的 isClickable 返回 true
 * - 默认折叠状态
 * - toggleExpand() 对多行描述的切换效果
 * - toggleExpand() 对单行描述无效果
 * - getDisplayLineCount() 折叠时返回 1
 * - getDisplayLineCount() 展开时返回总行数
 * - 单行描述的 getDisplayLineCount() 始终返回 1
 *
 * **注意：** 不测试 paint/render 方法（需要 mock Editor）。
 *
 * @since 1.0
 */
class PreCIDefectBlockRendererTest {

    // ==================== isClickable 测试 ====================

    /**
     * 测试单行描述 — isClickable() 返回 false
     *
     * 验证：
     * - 描述不包含换行符时，isClickable() 返回 false
     * - 单行描述不应显示展开/折叠指示器
     */
    @Test
    fun `test single line description isClickable returns false`() {
        val renderer = createRenderer(description = "单行描述信息")

        assertFalse(renderer.isClickable(), "单行描述的 isClickable() 应返回 false")
    }

    /**
     * 测试多行描述 — isClickable() 返回 true
     *
     * 验证：
     * - 描述包含换行符时，isClickable() 返回 true
     * - 多行描述应显示展开/折叠指示器
     */
    @Test
    fun `test multiline description isClickable returns true`() {
        val renderer = createRenderer(description = "第一行\n第二行")

        assertTrue(renderer.isClickable(), "多行描述的 isClickable() 应返回 true")
    }

    /**
     * 测试三行描述 — isClickable() 返回 true
     *
     * 验证：
     * - 超过两行的描述同样返回 true
     */
    @Test
    fun `test three line description isClickable returns true`() {
        val renderer = createRenderer(description = "第一行\n第二行\n第三行")

        assertTrue(renderer.isClickable(), "三行描述的 isClickable() 应返回 true")
    }

    // ==================== 默认状态测试 ====================

    /**
     * 测试默认状态为折叠（expanded = false）
     *
     * 验证：
     * - 新创建的渲染器默认处于折叠状态
     */
    @Test
    fun `test default state is collapsed`() {
        val renderer = createRenderer(description = "第一行\n第二行")

        assertFalse(renderer.isExpanded(), "默认状态应为折叠（expanded = false）")
    }

    /**
     * 测试单行描述的默认状态也为折叠
     *
     * 验证：
     * - 单行描述默认也是 expanded = false
     */
    @Test
    fun `test single line default state is collapsed`() {
        val renderer = createRenderer(description = "单行描述")

        assertFalse(renderer.isExpanded(), "单行描述的默认状态应为折叠")
    }

    // ==================== toggleExpand 测试 ====================

    /**
     * 测试多行描述的 toggleExpand() 正确切换状态
     *
     * 验证：
     * - 第一次调用 toggleExpand() 后变为展开
     * - 第二次调用 toggleExpand() 后恢复折叠
     */
    @Test
    fun `test toggleExpand correctly toggles state for multiline`() {
        val renderer = createRenderer(description = "第一行\n第二行")

        assertFalse(renderer.isExpanded(), "初始状态应为折叠")

        renderer.toggleExpand()
        assertTrue(renderer.isExpanded(), "第一次 toggleExpand() 后应为展开")

        renderer.toggleExpand()
        assertFalse(renderer.isExpanded(), "第二次 toggleExpand() 后应恢复折叠")
    }

    /**
     * 测试单行描述的 toggleExpand() 无效果
     *
     * 验证：
     * - 调用 toggleExpand() 后状态不变
     * - 多次调用也不会改变状态
     */
    @Test
    fun `test toggleExpand has no effect for single line`() {
        val renderer = createRenderer(description = "单行描述")

        assertFalse(renderer.isExpanded(), "初始状态应为折叠")

        renderer.toggleExpand()
        assertFalse(renderer.isExpanded(), "单行描述调用 toggleExpand() 后状态不应改变")

        renderer.toggleExpand()
        assertFalse(renderer.isExpanded(), "单行描述多次调用 toggleExpand() 后状态仍不应改变")
    }

    // ==================== getDisplayLineCount 测试 ====================

    /**
     * 测试折叠状态下 getDisplayLineCount() 返回 1
     *
     * 验证：
     * - 多行描述折叠时只显示一行
     */
    @Test
    fun `test getDisplayLineCount returns 1 when collapsed`() {
        val renderer = createRenderer(description = "第一行\n第二行\n第三行")

        assertEquals(1, renderer.getDisplayLineCount(), "折叠状态下 getDisplayLineCount() 应返回 1")
    }

    /**
     * 测试展开状态下 getDisplayLineCount() 返回总行数
     *
     * 验证：
     * - 展开后返回描述的实际行数
     */
    @Test
    fun `test getDisplayLineCount returns total line count when expanded`() {
        val renderer = createRenderer(description = "第一行\n第二行\n第三行")

        renderer.toggleExpand()
        assertEquals(3, renderer.getDisplayLineCount(), "展开状态下应返回总行数 3")
    }

    /**
     * 测试单行描述的 getDisplayLineCount() 始终返回 1
     *
     * 验证：
     * - 无论是否调用 toggleExpand()，单行描述始终返回 1
     */
    @Test
    fun `test single line getDisplayLineCount always returns 1`() {
        val renderer = createRenderer(description = "单行描述")

        assertEquals(1, renderer.getDisplayLineCount(), "单行描述折叠时应返回 1")

        renderer.toggleExpand()
        assertEquals(1, renderer.getDisplayLineCount(), "单行描述调用 toggleExpand() 后仍应返回 1")
    }

    /**
     * 测试两行描述展开后返回 2
     *
     * 验证：
     * - 两行描述展开后 getDisplayLineCount() 返回 2
     */
    @Test
    fun `test two line description expanded returns 2`() {
        val renderer = createRenderer(description = "第一行\n第二行")

        renderer.toggleExpand()
        assertEquals(2, renderer.getDisplayLineCount(), "两行描述展开后应返回 2")
    }

    // ==================== displayLines 数据处理测试 ====================

    /**
     * 测试首行包含 [toolName/checkerName] 前缀
     *
     * 验证：
     * - displayLines 的第一行包含工具名和规则名前缀
     * - 后续行为原始文本
     */
    @Test
    fun `test displayLines first line has prefix and subsequent lines are raw`() {
        val renderer = PreCIDefectBlockRenderer(
            Defect(
                toolName = "lint",
                checkerName = "rule1",
                description = "问题描述\n详细信息\n修复建议",
                filePath = "/test/file.kt",
                line = 1,
                severity = 2L
            )
        )

        val lines = renderer.displayLines

        assertEquals(3, lines.size, "应有 3 行")
        assertEquals("[lint/rule1] 问题描述", lines[0], "第一行应包含前缀")
        assertEquals("详细信息", lines[1], "第二行应为原始文本")
        assertEquals("修复建议", lines[2], "第三行应为原始文本")
    }

    /**
     * 测试空描述 — 单行，isClickable() 返回 false
     *
     * 验证：
     * - description = "" 时，displayLines 为单行
     * - isClickable() 返回 false（无可展开内容）
     */
    @Test
    fun `test empty description is single line and not clickable`() {
        val renderer = createRenderer(description = "")

        assertEquals(1, renderer.displayLines.size, "空描述应为单行")
        assertFalse(renderer.isClickable(), "空描述的 isClickable() 应返回 false")
    }

    /**
     * 测试描述以换行符结尾 — 2 行，isClickable() 返回 true
     *
     * 验证：
     * - description = "第一行\n" 时，displayLines 为 2 行
     * - isClickable() 返回 true
     */
    @Test
    fun `test description ending with newline is two lines and clickable`() {
        val renderer = createRenderer(description = "第一行\n")

        assertEquals(2, renderer.displayLines.size, "以换行符结尾的描述应为 2 行")
        assertTrue(renderer.isClickable(), "两行描述的 isClickable() 应返回 true")
    }

    /**
     * 测试 Windows 风格换行符 \r\n
     *
     * 验证：
     * - description = "第一行\r\n第二行" 时，正确拆分为 2 行
     * - isClickable() 返回 true
     */
    @Test
    fun `test crlf newlines are split correctly`() {
        val renderer = createRenderer(description = "第一行\r\n第二行")

        assertEquals(2, renderer.displayLines.size, "\\r\\n 换行应拆分为 2 行")
        assertTrue(renderer.isClickable(), "两行描述的 isClickable() 应返回 true")
    }

    /**
     * 测试全空行描述
     *
     * 验证：
     * - description = "\n\n" 时，displayLines 为 3 行
     * - isClickable() 返回 true
     */
    @Test
    fun `test all empty lines is three lines and clickable`() {
        val renderer = createRenderer(description = "\n\n")

        assertEquals(3, renderer.displayLines.size, "\\n\\n 应产生 3 行")
        assertTrue(renderer.isClickable(), "多行描述的 isClickable() 应返回 true")
    }

    /**
     * 测试单行描述的 displayLines
     *
     * 验证：
     * - displayLines 只有一个元素
     * - 该元素包含 [toolName/checkerName] 前缀
     */
    @Test
    fun `test single line displayLines has prefix`() {
        val renderer = PreCIDefectBlockRenderer(
            Defect(
                toolName = "tool",
                checkerName = "checker",
                description = "简单描述",
                filePath = "/test/file.kt",
                line = 1,
                severity = 4L
            )
        )

        val lines = renderer.displayLines

        assertEquals(1, lines.size, "单行描述应只有 1 行")
        assertEquals("[tool/checker] 简单描述", lines[0], "单行描述应包含前缀")
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用的渲染器
     *
     * @param toolName 工具名称，默认为 "test-tool"
     * @param checkerName 规则名称，默认为 "test-checker"
     * @param description 描述信息
     * @param severity 严重程度，默认为 2（一般）
     * @return PreCIDefectBlockRenderer 对象
     */
    private fun createRenderer(
        toolName: String = "test-tool",
        checkerName: String = "test-checker",
        description: String = "test description",
        severity: Long = 2L
    ): PreCIDefectBlockRenderer {
        return PreCIDefectBlockRenderer(
            Defect(
                toolName = toolName,
                checkerName = checkerName,
                description = description,
                filePath = "/test/file.kt",
                line = 1,
                severity = severity
            )
        )
    }
}
