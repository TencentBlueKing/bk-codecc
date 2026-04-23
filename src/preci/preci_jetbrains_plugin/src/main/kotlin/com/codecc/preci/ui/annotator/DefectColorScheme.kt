package com.codecc.preci.ui.annotator

import com.codecc.preci.api.model.response.Defect
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Color
import javax.swing.Icon

/**
 * PreCI 告警颜色方案
 *
 * 根据告警的严重程度（severity）提供不同的颜色、图标和文本属性。
 *
 * **严重程度分类：**
 * - **严重告警** (severity=1): 红色氛围，表示需要立即修复的严重问题
 * - **一般告警** (severity=2): 黄色氛围，表示应该修复的常规问题
 * - **提示告警** (severity=4): 蓝绿色氛围，表示建议性的优化提示
 *
 * **颜色使用：**
 * - 波浪下划线颜色：用于在代码行上标记告警
 * - 图标：在编辑器左侧 Gutter 区域显示
 * - 滚动条标记颜色：在编辑器右侧滚动条上显示告警位置
 *
 * **设计原则：**
 * - 遵循 IntelliJ Platform 的设计规范
 * - 支持亮色和暗色主题
 * - 颜色对比度足够，确保可读性
 * - 与 IDE 内置的错误/警告/信息高亮保持一致的视觉风格
 *
 * @since 1.0
 */
object DefectColorScheme {

    /**
     * 严重告警的波浪线颜色（红色）
     *
     * 使用 JBColor 以支持亮色和暗色主题：
     * - 亮色主题：鲜艳的红色
     * - 暗色主题：稍微柔和的红色
     */
    private val SERIOUS_COLOR = JBColor(
        Color(0xFF, 0x00, 0x00),  // 亮色主题：鲜红色
        Color(0xFF, 0x66, 0x66)   // 暗色主题：柔和红色
    )

    /**
     * 一般告警的波浪线颜色（黄色）
     *
     * 使用 JBColor 以支持亮色和暗色主题：
     * - 亮色主题：警示黄色
     * - 暗色主题：稍微柔和的黄色
     */
    private val NORMAL_COLOR = JBColor(
        Color(0xFF, 0xAA, 0x00),  // 亮色主题：橙黄色
        Color(0xFF, 0xCC, 0x00)   // 暗色主题：柔和黄色
    )

    /**
     * 提示告警的波浪线颜色（蓝绿色）
     *
     * 使用 JBColor 以支持亮色和暗色主题：
     * - 亮色主题：青绿色
     * - 暗色主题：稍微明亮的青绿色
     */
    private val INFO_COLOR = JBColor(
        Color(0x00, 0x99, 0x99),  // 亮色主题：青绿色
        Color(0x00, 0xCC, 0xCC)   // 暗色主题：明亮青绿色
    )

    /**
     * 根据严重程度获取波浪线颜色
     *
     * @param severity 严重程度：1=严重、2=一般、4=提示
     * @return 对应的颜色
     */
    fun getEffectColor(severity: Long): JBColor {
        return when (severity) {
            1L -> SERIOUS_COLOR  // 严重：红色
            2L -> NORMAL_COLOR   // 一般：黄色
            4L -> INFO_COLOR     // 提示：蓝绿色
            else -> INFO_COLOR   // 默认：提示：蓝绿色
        }
    }

    /**
     * 根据严重程度获取 Gutter 图标
     *
     * 使用 IntelliJ 内置图标，保持与 IDE 风格一致。
     *
     * @param severity 严重程度：1=严重、2=一般、4=提示
     * @return 对应的图标
     */
    fun getIcon(severity: Long): Icon {
        return when (severity) {
            1L -> AllIcons.General.Error         // 严重：错误图标（红色）
            2L -> AllIcons.General.Warning       // 一般：警告图标（黄色）
            4L -> AllIcons.General.Information   // 提示：信息图标（蓝色）
            else -> AllIcons.General.Information // 默认：提示：信息图标（蓝色）
        }
    }

    /**
     * 根据严重程度获取文本属性
     *
     * 包含波浪下划线效果和颜色。
     *
     * @param severity 严重程度：1=严重、2=一般、4=提示
     * @return 对应的文本属性
     */
    fun getTextAttributes(severity: Long): TextAttributes {
        return TextAttributes().apply {
            effectType = EffectType.WAVE_UNDERSCORE
            effectColor = getEffectColor(severity)
        }
    }

    /**
     * 根据缺陷获取波浪线颜色
     *
     * 便捷方法，直接从 Defect 对象获取颜色。
     *
     * @param defect 缺陷对象
     * @return 对应的颜色
     */
    fun getEffectColor(defect: Defect): JBColor {
        return getEffectColor(defect.severity)
    }

    /**
     * 根据缺陷获取 Gutter 图标
     *
     * 便捷方法，直接从 Defect 对象获取图标。
     *
     * @param defect 缺陷对象
     * @return 对应的图标
     */
    fun getIcon(defect: Defect): Icon {
        return getIcon(defect.severity)
    }

    /**
     * 根据缺陷获取文本属性
     *
     * 便捷方法，直接从 Defect 对象获取文本属性。
     *
     * @param defect 缺陷对象
     * @return 对应的文本属性
     */
    fun getTextAttributes(defect: Defect): TextAttributes {
        return getTextAttributes(defect.severity)
    }

    /**
     * 获取严重程度的描述文本
     *
     * 用于日志记录和调试。
     *
     * @param severity 严重程度
     * @return 描述文本
     */
    fun getSeverityDescription(severity: Long): String {
        return when (severity) {
            1L -> "严重 (SERIOUS)"
            2L -> "一般 (NORMAL)"
            4L -> "提示 (INFO)"
            else -> "提示 (INFO)"
        }
    }

    /**
     * 获取 Inlay Hints 的背景和文字颜色
     *
     * 用于在代码行上方显示块状告警提示。
     * 支持亮色和暗色主题，自动适配。
     *
     * **颜色方案：**
     * - severity=1（严重）: 红色背景 + 红色文字
     * - severity=2（一般）: 黄色背景 + 黄色文字
     * - 其他（提示）: 绿色背景 + 绿色文字
     *
     * **设计考虑：**
     * - 使用半透明背景，避免过度遮挡代码
     * - 文字颜色对比度足够，确保可读性
     * - 亮色主题使用深色文字，暗色主题使用浅色文字
     *
     * @param severity 严重程度：1=严重、2=一般、其他=提示
     * @return Pair<背景颜色, 文字颜色>
     */
    fun getInlayColors(severity: Long): Pair<JBColor, JBColor> {
        return when (severity) {
            1L -> {
                // 严重告警：红色方案
                val backgroundColor = JBColor(
                    Color(255, 235, 238),  // 亮色主题：浅红色背景
                    Color(80, 30, 30)      // 暗色主题：深红色背景
                )
                val textColor = JBColor(
                    Color(180, 0, 0),      // 亮色主题：深红色文字
                    Color(255, 100, 100)   // 暗色主题：浅红色文字
                )
                Pair(backgroundColor, textColor)
            }
            2L -> {
                // 一般告警：黄色方案
                val backgroundColor = JBColor(
                    Color(255, 250, 230),  // 亮色主题：浅黄色背景
                    Color(80, 70, 0)       // 暗色主题：深黄色背景
                )
                val textColor = JBColor(
                    Color(180, 120, 0),    // 亮色主题：深黄色文字
                    Color(255, 200, 0)     // 暗色主题：浅黄色文字
                )
                Pair(backgroundColor, textColor)
            }
            else -> {
                // 提示告警：绿色方案
                val backgroundColor = JBColor(
                    Color(230, 250, 240),  // 亮色主题：浅绿色背景
                    Color(30, 60, 40)      // 暗色主题：深绿色背景
                )
                val textColor = JBColor(
                    Color(0, 130, 60),     // 亮色主题：深绿色文字
                    Color(100, 220, 150)   // 暗色主题：浅绿色文字
                )
                Pair(backgroundColor, textColor)
            }
        }
    }

    /**
     * 根据缺陷获取 Inlay Hints 颜色
     *
     * 便捷方法，直接从 Defect 对象获取 Inlay Hints 的颜色方案。
     *
     * @param defect 缺陷对象
     * @return Pair<背景颜色, 文字颜色>
     */
    fun getInlayColors(defect: Defect): Pair<JBColor, JBColor> {
        return getInlayColors(defect.severity)
    }
}


