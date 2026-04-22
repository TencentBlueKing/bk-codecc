package com.codecc.preci.ui.toolwindow

import com.codecc.preci.BaseTest
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * LocalCodeCheckPanel 单元测试
 *
 * 测试本地代码检查面板的基本功能和 UI 组件。
 *
 * **测试范围：**
 * - 面板的创建和初始化
 * - UI 组件的基本属性和布局
 * - 扫描范围按钮和共用结果面板结构
 * - 工具栏按钮
 * - 公共方法的功能验证
 *
 * **注意事项：**
 * - 该测试不依赖真实的 IntelliJ Platform 环境
 * - 使用 Mock 对象模拟 Project 实例
 * - 主要验证组件的结构和基本功能
 * - 不测试实际的扫描逻辑（由 ScanService 测试覆盖）
 *
 * @since 1.0
 */
@DisplayName("LocalCodeCheckPanel 测试")
class LocalCodeCheckPanelTest : BaseTest() {

    private lateinit var mockProject: Project
    private lateinit var testScope: CoroutineScope
    private lateinit var checkPanel: LocalCodeCheckPanel

    /**
     * 在每个测试前执行
     *
     * 初始化 Mock 对象和检查面板实例
     */
    @BeforeEach
    fun setUp() {
        // 创建 Mock Project
        mockProject = mock(Project::class.java)
        
        // 模拟 project.name 和 basePath 属性
        org.mockito.Mockito.`when`(mockProject.name).thenReturn("TestProject")
        org.mockito.Mockito.`when`(mockProject.basePath).thenReturn("/test/project")
        
        // 创建测试用的协程作用域
        testScope = CoroutineScope(SupervisorJob())
        
        // 创建检查面板实例（不需要回调参数）
        checkPanel = LocalCodeCheckPanel(
            mockProject,
            testScope
        )
    }

    @Test
    @DisplayName("测试检查面板创建成功")
    fun testPanelCreation() {
        // 验证检查面板实例创建成功
        assertNotNull(checkPanel)
    }

    @Test
    @DisplayName("测试获取内容组件")
    fun testGetContent() {
        // 获取主面板组件
        val content = checkPanel.getContent()

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
        val content = checkPanel.getContent()

        // 验证主面板可见
        assertTrue(content.isVisible)

        // 验证主面板已启用
        assertTrue(content.isEnabled)
    }

    @Test
    @DisplayName("测试主面板布局")
    fun testMainPanelLayout() {
        val content = checkPanel.getContent() as JPanel

        // 验证主面板使用 BorderLayout
        assertTrue(content.layout is java.awt.BorderLayout)

        // 验证主面板有子组件
        assertTrue(content.componentCount > 0)
    }

    @Test
    @DisplayName("测试主面板包含工具栏和内容区域")
    fun testMainPanelComponents() {
        val content = checkPanel.getContent() as JPanel
        val layout = content.layout as java.awt.BorderLayout

        var hasWestComponent = false
        var hasCenterComponent = false

        for (component in content.components) {
            val constraints = layout.getConstraints(component)
            if (constraints == java.awt.BorderLayout.WEST) {
                hasWestComponent = true
            }
            if (constraints == java.awt.BorderLayout.CENTER) {
                hasCenterComponent = true
            }
        }

        assertTrue(hasWestComponent, "检查面板应该包含左侧工具栏组件")
        assertTrue(hasCenterComponent, "检查面板应该包含中央内容区域组件")
    }

    @Test
    @DisplayName("测试 onScanComplete 方法不抛出异常")
    fun testOnScanCompleteMethod() {
        // 调用 refresh 方法不应抛出异常
        assertDoesNotThrow {
            checkPanel.refresh()
        }
    }

    @Test
    @DisplayName("测试多次调用 getContent 返回同一实例")
    fun testGetContentReturnsSameInstance() {
        // 多次调用 getContent
        val content1 = checkPanel.getContent()
        val content2 = checkPanel.getContent()

        // 验证返回的是同一个实例
        assertSame(content1, content2, "多次调用 getContent 应返回同一个实例")
    }

    @Test
    @DisplayName("测试面板组件树的深度")
    fun testComponentTreeDepth() {
        val content = checkPanel.getContent() as JPanel

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

        // 验证有嵌套组件
        assertTrue(hasNestedComponents, "检查面板应该有多层组件嵌套")
    }

    @Test
    @DisplayName("测试检查面板在不同项目实例下的独立性")
    fun testPanelIndependence() {
        // 创建第二个 Mock Project
        val mockProject2 = mock(Project::class.java)
        org.mockito.Mockito.`when`(mockProject2.name).thenReturn("TestProject2")
        org.mockito.Mockito.`when`(mockProject2.basePath).thenReturn("/test/project2")

        // 创建第二个检查面板实例
        val checkPanel2 = LocalCodeCheckPanel(mockProject2, testScope)

        // 获取两个检查面板的内容
        val content1 = checkPanel.getContent()
        val content2 = checkPanel2.getContent()

        // 验证两个检查面板的内容是不同的实例
        assertNotSame(content1, content2, "不同项目的检查面板应该是独立的实例")
    }

    @Test
    @DisplayName("测试面板可以正常调用 refresh 多次")
    fun testMultipleRefresh() {
        // 多次调用 refresh 不应抛出异常
        assertDoesNotThrow {
            checkPanel.refresh()
            checkPanel.refresh()
            checkPanel.refresh()
        }
    }
}

