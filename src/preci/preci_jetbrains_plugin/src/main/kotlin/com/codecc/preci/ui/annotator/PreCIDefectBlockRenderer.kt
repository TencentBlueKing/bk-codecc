package com.codecc.preci.ui.annotator

import com.codecc.preci.api.model.response.Defect
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle

/**
 * PreCI 告警块状渲染器
 *
 * 自定义渲染器，用于在代码行上方显示彩色的告警信息块。
 * 支持多行描述的展开/折叠显示。
 *
 * **显示格式：**
 * - 第一行：`[工具名/规则名] 描述首行`
 * - 后续行（展开时）：原始描述文本
 *
 * **多行行为：**
 * - 折叠时在首行文字前显示 "▶" 指示器
 * - 展开时在首行文字前显示 "▼" 指示器
 * - 单行描述不显示指示器，不可点击
 *
 * **颜色方案：**
 * - severity=1（严重）：红色背景 + 红色文字
 * - severity=2（一般）：黄色背景 + 黄色文字
 * - 其他（提示）：绿色背景 + 绿色文字
 *
 * @param defect 要显示的告警对象
 * @since 1.0
 */
class PreCIDefectBlockRenderer(
    private val defect: Defect
) : EditorCustomElementRenderer {

    /**
     * 按换行符拆分后的显示行列表
     *
     * 第一行包含 `[toolName/checkerName] ` 前缀，后续行为原始文本。
     */
    val displayLines: List<String> = buildDisplayLines()

    /** 是否包含多行描述 */
    private val hasMultipleLines: Boolean = displayLines.size > 1

    // 展开/折叠状态，默认折叠
    private var expanded: Boolean = false

    // 折叠指示器（显示在首行文字前面）
    private val collapsedIndicator = "▶ "
    private val expandedIndicator = "▼ "

    // 行间距（像素）
    private val lineSpacing = 2

    // 获取颜色方案
    private val colorPair = DefectColorScheme.getInlayColors(defect.severity)
    private val backgroundColor = colorPair.first
    private val textColor = colorPair.second

    // 内边距
    private val paddingHorizontal = 8
    private val paddingVertical = 4

    // 圆角矩形半径
    private val cornerRadius = 8

    /**
     * 切换展开/折叠状态
     *
     * 仅在描述包含多行时有效，单行描述调用此方法无任何效果。
     */
    fun toggleExpand() {
        if (hasMultipleLines) {
            expanded = !expanded
        }
    }

    /**
     * 获取当前展开状态
     *
     * @return 是否处于展开状态
     */
    fun isExpanded(): Boolean = expanded

    /**
     * 判断是否可点击（即是否有多行内容可展开）
     *
     * @return 多行描述返回 true，单行描述返回 false
     */
    fun isClickable(): Boolean = hasMultipleLines

    /**
     * 获取当前应显示的行数
     *
     * 折叠时返回 1，展开时返回全部行数。
     *
     * @return 当前显示行数
     */
    fun getDisplayLineCount(): Int {
        return if (expanded) displayLines.size else 1
    }

    /**
     * 计算元素的宽度
     *
     * - 单行：文本宽度 + 内边距
     * - 折叠（多行）：折叠指示器宽度 + 首行宽度 + 内边距
     * - 展开：指示器宽度 + 所有行的最大宽度 + 内边距
     *
     * @param inlay Inlay 对象
     * @return 宽度（像素）
     */
    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val editor = inlay.editor
        val fontMetrics = editor.contentComponent.getFontMetrics(getFont(editor))

        if (!hasMultipleLines) {
            val textWidth = fontMetrics.stringWidth(displayLines[0])
            return textWidth + paddingHorizontal * 2
        }

        val indicator = if (expanded) expandedIndicator else collapsedIndicator
        val indicatorWidth = fontMetrics.stringWidth(indicator)

        val linesToMeasure = if (expanded) displayLines else listOf(displayLines[0])
        val maxLineWidth = linesToMeasure.maxOf { fontMetrics.stringWidth(it) }

        val editorWidth = editor.scrollingModel.visibleArea.width
        val calculatedWidth = maxLineWidth + indicatorWidth + paddingHorizontal * 2
        return if (editorWidth > 0) minOf(calculatedWidth, editorWidth) else calculatedWidth
    }

    /**
     * 计算元素的高度
     *
     * - 折叠/单行：单行高度 + 内边距
     * - 展开：行数 * 行高 + (行数-1) * 行间距 + 内边距
     *
     * @param inlay Inlay 对象
     * @return 高度（像素）
     */
    override fun calcHeightInPixels(inlay: Inlay<*>): Int {
        val editor = inlay.editor
        val fontMetrics = editor.contentComponent.getFontMetrics(getFont(editor))
        val lineHeight = fontMetrics.height
        val lineCount = getDisplayLineCount()

        return if (lineCount == 1) {
            lineHeight + paddingVertical * 2
        } else {
            lineCount * lineHeight + (lineCount - 1) * lineSpacing + paddingVertical * 2
        }
    }

    /**
     * 绘制元素
     *
     * 绘制带圆角的背景矩形和文本。多行描述在首行文字前显示展开/折叠指示器。
     *
     * @param inlay Inlay 对象
     * @param g Graphics 对象
     * @param targetRegion 目标区域
     * @param textAttributes 文本属性
     */
    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val editor = inlay.editor

        // 绘制圆角背景
        g.color = backgroundColor
        g.fillRoundRect(
            targetRegion.x,
            targetRegion.y,
            targetRegion.width,
            targetRegion.height,
            cornerRadius,
            cornerRadius
        )

        // 绘制文本
        g.color = textColor
        g.font = getFont(editor)

        val fontMetrics = g.fontMetrics
        val textX = targetRegion.x + paddingHorizontal
        val lineHeight = fontMetrics.height

        val indicatorWidth = if (hasMultipleLines) {
            val indicator = if (expanded) expandedIndicator else collapsedIndicator
            fontMetrics.stringWidth(indicator)
        } else 0

        val lineCount = getDisplayLineCount()
        for (i in 0 until lineCount) {
            val textY = targetRegion.y + paddingVertical + fontMetrics.ascent + i * (lineHeight + lineSpacing)
            val lineText = displayLines[i]

            if (i == 0 && hasMultipleLines) {
                val indicator = if (expanded) expandedIndicator else collapsedIndicator
                g.drawString(indicator, textX, textY)
                g.drawString(lineText, textX + indicatorWidth, textY)
            } else {
                g.drawString(lineText, textX, textY)
            }
        }
    }

    /**
     * 获取字体
     *
     * 使用编辑器的默认字体。
     *
     * @param editor 编辑器对象
     * @return 字体对象
     */
    private fun getFont(editor: Editor): Font {
        return editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)
    }

    /**
     * 构建显示行列表
     *
     * 将 defect.description 按换行符拆分，第一行添加 `[toolName/checkerName] ` 前缀。
     *
     * @return 处理后的显示行列表
     */
    private fun buildDisplayLines(): List<String> {
        val rawLines = defect.description.split(Regex("\\r?\\n"))
        return rawLines.mapIndexed { index, line ->
            if (index == 0) {
                "[${defect.toolName}/${defect.checkerName}] $line"
            } else {
                line
            }
        }
    }
}
