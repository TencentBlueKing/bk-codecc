package com.codecc.preci.ui.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * PreCI 工具窗口工厂类
 *
 * 实现 [ToolWindowFactory] 接口，负责创建 PreCI 工具窗口。
 * 实现 [DumbAware] 表示该工具窗口在 IDE 索引时仍可用。
 *
 * **功能：**
 * - 创建 PreCI 工具窗口的主内容面板
 * - 初始化工具窗口的布局和组件
 * - 注册到 IDE 的工具窗口系统
 * - 提供工具窗口实例的访问接口
 *
 * **工具窗口特性：**
 * - 位置：默认显示在 IDE 右侧
 * - 图标：使用 PreCI 插件图标
 * - 标题：显示为 "PreCI"
 *
 * **使用方式：**
 * 该工厂类通过 plugin.xml 中的 toolWindow 扩展点注册：
 * ```xml
 * <toolWindow 
 *     id="PreCI" 
 *     anchor="right" 
 *     factoryClass="com.codecc.preci.ui.toolwindow.PreCIToolWindowFactory"
 *     icon="AllIcons.Toolwindows.ToolWindowBuild"/>
 * ```
 *
 * @since 1.0
 * @see PreCIToolWindow
 */
class PreCIToolWindowFactory : ToolWindowFactory, DumbAware {

    companion object {
        /**
         * 存储各项目的 PreCIToolWindow 实例
         */
        private val toolWindowInstances = ConcurrentHashMap<Project, PreCIToolWindow>()

        /**
         * 获取指定项目的 PreCIToolWindow 实例
         *
         * @param project 项目实例
         * @return PreCIToolWindow 实例，如果工具窗口未创建则返回 null
         */
        fun getToolWindow(project: Project): PreCIToolWindow? {
            return toolWindowInstances[project]
        }
    }

    /**
     * 创建工具窗口内容
     *
     * 当用户打开工具窗口时，IDE 会调用此方法创建工具窗口的内容。
     * 该方法负责：
     * 1. 创建工具窗口的主面板（[PreCIToolWindow]）
     * 2. 将主面板包装为 IDE 内容（Content）
     * 3. 添加到工具窗口中
     * 4. 将实例保存到 Map 中供后续访问
     *
     * **设计说明：**
     * - 每个 [Project] 都会有独立的工具窗口实例
     * - 工具窗口内容会在首次显示时创建，不会随着窗口的隐藏而销毁
     * - 使用 [ContentFactory] 创建符合 IDE 规范的内容实例
     *
     * @param project 当前项目实例
     * @param toolWindow 工具窗口实例
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 创建工具窗口主面板
        val toolWindowPanel = PreCIToolWindow(project)
        
        // 将实例保存到 Map 中，供其他地方访问
        toolWindowInstances[project] = toolWindowPanel
        
        // 获取内容工厂
        val contentFactory = ContentFactory.getInstance()
        
        // 创建内容并添加到工具窗口
        val content = contentFactory.createContent(
            toolWindowPanel.getContent(),  // 主面板组件
            "",                             // 标签名称，空字符串表示不显示标签
            false                           // 是否可关闭，false 表示不显示关闭按钮
        )
        
        // 将内容添加到工具窗口
        toolWindow.contentManager.addContent(content)
    }

    /**
     * 检查工具窗口是否应该可用
     *
     * 用于决定工具窗口是否应该在项目中显示。
     * 默认返回 true，表示对所有项目都可用。
     *
     * **扩展点：**
     * 如果需要根据项目类型动态判断（例如只在特定语言项目中显示），
     * 可以覆盖此方法并实现自定义逻辑。
     *
     * @param project 当前项目实例
     * @return true 表示工具窗口可用，false 表示隐藏工具窗口
     */
    override fun shouldBeAvailable(project: Project): Boolean {
        // 所有项目都可以使用 PreCI 工具窗口
        return true
    }
}

