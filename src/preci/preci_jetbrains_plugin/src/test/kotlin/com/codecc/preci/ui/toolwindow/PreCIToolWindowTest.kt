package com.codecc.preci.ui.toolwindow

import com.codecc.preci.BaseTest
import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * PreCIToolWindow 单元测试
 *
 * 测试 PreCI 工具窗口主面板的基本功能和 UI 组件。
 *
 * **测试范围：**
 * - 工具窗口主面板的创建和初始化
 * - UI 组件的基本属性和布局
 * - 公共方法的功能验证
 *
 * **注意事项：**
 * - 该测试不依赖真实的 IntelliJ Platform 环境
 * - 使用 Mock 对象模拟 Project 实例
 * - 主要验证组件的结构和基本功能
 *
 * @since 1.0
 */
@DisplayName("PreCIToolWindow 测试")
class PreCIToolWindowTest : BaseTest() {

    private lateinit var mockProject: Project
    private lateinit var toolWindow: PreCIToolWindow

    /**
     * 在每个测试前执行
     *
     * 初始化 Mock 对象和工具窗口实例
     */
    @BeforeEach
    fun setUp() {
        // 创建 Mock Project
        mockProject = mock(Project::class.java)
        
        // 模拟 project.name 属性
        org.mockito.Mockito.`when`(mockProject.name).thenReturn("TestProject")
        
        // 创建工具窗口实例
        toolWindow = PreCIToolWindow(mockProject)
    }

    @Test
    @DisplayName("测试工具窗口创建成功")
    fun testToolWindowCreation() {
        // 验证工具窗口实例创建成功
        assertNotNull(toolWindow)
    }

    @Test
    @DisplayName("测试获取内容组件")
    fun testGetContent() {
        // 获取主面板组件
        val content = toolWindow.getContent()

        // 验证返回的组件不为空
        assertNotNull(content)

        // 验证返回的是 JComponent 实例
        assertTrue(content is JComponent)

        // 验证返回的是 JPanel 实例
        assertTrue(content is JPanel)
    }

    @Test
    @DisplayName("测试主面板基本属性")
    fun testMainPanelProperties() {
        val content = toolWindow.getContent()

        // 验证主面板可见
        assertTrue(content.isVisible)

        // 验证主面板已启用
        assertTrue(content.isEnabled)

        // 验证主面板有边框（设置了边距）
        assertNotNull(content.border)
    }

    @Test
    @DisplayName("测试主面板布局")
    fun testMainPanelLayout() {
        val content = toolWindow.getContent() as JPanel

        // 验证主面板使用 BorderLayout
        assertTrue(content.layout is java.awt.BorderLayout)

        // 验证主面板有子组件
        assertTrue(content.componentCount > 0)
    }

    @Test
    @DisplayName("测试主面板包含工具栏和内容区域")
    fun testMainPanelComponents() {
        val content = toolWindow.getContent() as JPanel
        val layout = content.layout as java.awt.BorderLayout

        // 获取各个位置的组件
        var hasNorthComponent = false
        var hasCenterComponent = false

        for (component in content.components) {
            val constraints = layout.getConstraints(component)
            if (constraints == java.awt.BorderLayout.NORTH) {
                hasNorthComponent = true
            }
            if (constraints == java.awt.BorderLayout.CENTER) {
                hasCenterComponent = true
            }
        }

        // 验证有顶部组件（工具栏）
        assertTrue(hasNorthComponent, "工具窗口应该包含顶部工具栏组件")

        // 验证有中央组件（内容区域）
        assertTrue(hasCenterComponent, "工具窗口应该包含中央内容区域组件")
    }

    @Test
    @DisplayName("测试 refresh 方法不抛出异常")
    fun testRefreshMethod() {
        // 调用 refresh 方法不应抛出异常
        assertDoesNotThrow {
            toolWindow.refresh()
        }
    }

    @Test
    @DisplayName("测试 clear 方法不抛出异常")
    fun testClearMethod() {
        // 调用 clear 方法不应抛出异常
        assertDoesNotThrow {
            toolWindow.clear()
        }
    }

    @Test
    @DisplayName("测试多次调用 getContent 返回同一实例")
    fun testGetContentReturnsSameInstance() {
        // 多次调用 getContent
        val content1 = toolWindow.getContent()
        val content2 = toolWindow.getContent()

        // 验证返回的是同一个实例
        assertSame(content1, content2, "多次调用 getContent 应返回同一个实例")
    }

    @Test
    @DisplayName("测试工具窗口组件树的深度")
    fun testComponentTreeDepth() {
        val content = toolWindow.getContent() as JPanel

        // 验证主面板至少有一层子组件
        assertTrue(content.componentCount > 0)

        // 检查是否有多层嵌套
        var hasNestedComponents = false
        for (component in content.components) {
            if (component is JPanel && component.componentCount > 0) {
                hasNestedComponents = true
                break
            }
        }

        // 验证有嵌套组件（工具栏或内容区域至少有一个包含子组件）
        assertTrue(hasNestedComponents, "工具窗口应该有多层组件嵌套")
    }

    @Test
    @DisplayName("测试工具窗口在不同项目实例下的独立性")
    fun testToolWindowIndependence() {
        // 创建第二个 Mock Project
        val mockProject2 = mock(Project::class.java)
        org.mockito.Mockito.`when`(mockProject2.name).thenReturn("TestProject2")
        org.mockito.Mockito.`when`(mockProject2.basePath).thenReturn("/test/project2")

        // 创建第二个工具窗口实例
        val toolWindow2 = PreCIToolWindow(mockProject2)

        // 获取两个工具窗口的内容
        val content1 = toolWindow.getContent()
        val content2 = toolWindow2.getContent()

        // 验证两个工具窗口的内容是不同的实例
        assertNotSame(content1, content2, "不同项目的工具窗口应该是独立的实例")
    }

    @Test
    @DisplayName("测试 dispose 方法不抛出异常")
    fun testDisposeMethod() {
        // 调用 dispose 方法不应抛出异常
        assertDoesNotThrow {
            toolWindow.dispose()
        }
    }
}

