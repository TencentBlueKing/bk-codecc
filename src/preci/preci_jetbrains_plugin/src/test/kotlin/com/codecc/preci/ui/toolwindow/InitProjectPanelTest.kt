package com.codecc.preci.ui.toolwindow

import com.codecc.preci.BaseTest
import com.codecc.preci.api.model.response.InitResponse
import com.codecc.preci.service.scan.InitResult
import com.codecc.preci.service.scan.ScanService
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * InitProjectPanel 单元测试
 *
 * 测试初始化项目面板的基本功能和 UI 组件。
 *
 * **测试范围：**
 * - 面板的创建和初始化
 * - UI 组件的基本属性和布局
 * - 初始化按钮功能
 * - 状态标签更新
 * - 公共方法的功能验证
 *
 * **注意事项：**
 * - 该测试不依赖真实的 IntelliJ Platform 环境
 * - 使用 Mock 对象模拟 Project 和 ScanService
 * - 主要验证组件的结构和基本功能
 * - 不测试实际的 API 调用（由 ScanService 测试覆盖）
 *
 * @since 1.0
 */
@DisplayName("InitProjectPanel 测试")
class InitProjectPanelTest : BaseTest() {

    private lateinit var mockProject: Project
    private lateinit var testScope: CoroutineScope
    private lateinit var initPanel: InitProjectPanel

    /**
     * 在每个测试前执行
     *
     * 初始化 Mock 对象和初始化面板实例
     */
    @BeforeEach
    fun setUp() {
        // 创建 Mock Project
        mockProject = mock(Project::class.java)

        // 模拟 project.name 和 basePath 属性
        `when`(mockProject.name).thenReturn("TestProject")
        `when`(mockProject.basePath).thenReturn("/test/project")

        // 创建测试用的协程作用域
        testScope = CoroutineScope(SupervisorJob())

        // 创建初始化面板实例
        initPanel = InitProjectPanel(mockProject, testScope)
    }

    @Test
    @DisplayName("测试初始化面板创建成功")
    fun testPanelCreation() {
        // 验证初始化面板实例创建成功
        assertNotNull(initPanel)
    }

    @Test
    @DisplayName("测试获取内容组件")
    fun testGetContent() {
        // 获取主面板组件
        val content = initPanel.getContent()

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
        val content = initPanel.getContent()

        // 验证主面板可见
        assertTrue(content.isVisible)

        // 验证主面板已启用
        assertTrue(content.isEnabled)

        // 验证主面板有边框（设置了边距和分隔线）
        assertNotNull(content.border)
    }

    @Test
    @DisplayName("测试主面板布局")
    fun testMainPanelLayout() {
        val content = initPanel.getContent() as JPanel

        // 验证主面板使用 BorderLayout
        assertTrue(content.layout is java.awt.BorderLayout)

        // 验证主面板有子组件
        assertTrue(content.componentCount > 0)
    }

    @Test
    @DisplayName("测试面板包含初始化按钮")
    fun testPanelContainsInitButton() {
        val content = initPanel.getContent() as JPanel

        // 递归查找所有 JButton 组件
        val buttons = findComponentsOfType<JButton>(content)

        // 验证至少有一个按钮（初始化按钮）
        assertTrue(buttons.isNotEmpty(), "初始化面板应该包含至少一个按钮")

        // 验证存在文本为"初始化"的按钮
        val initButton = buttons.firstOrNull { it.text == "初始化" }
        assertNotNull(initButton, "应该存在文本为'初始化'的按钮")
    }

    @Test
    @DisplayName("测试初始化按钮的基本属性")
    fun testInitButtonProperties() {
        val content = initPanel.getContent() as JPanel
        val buttons = findComponentsOfType<JButton>(content)
        val initButton = buttons.firstOrNull { it.text == "初始化" }

        assertNotNull(initButton, "应该存在初始化按钮")

        // 验证按钮初始状态已启用
        assertTrue(initButton!!.isEnabled, "初始化按钮初始状态应该是启用的")

        // 验证按钮有 tooltip
        assertNotNull(initButton.toolTipText, "初始化按钮应该有工具提示")
    }

    @Test
    @DisplayName("测试多次调用 getContent 返回同一实例")
    fun testGetContentReturnsSameInstance() {
        // 多次调用 getContent
        val content1 = initPanel.getContent()
        val content2 = initPanel.getContent()

        // 验证返回的是同一个实例
        assertSame(content1, content2, "多次调用 getContent 应返回同一个实例")
    }

    @Test
    @DisplayName("测试初始化面板在不同项目实例下的独立性")
    fun testPanelIndependence() {
        // 创建第二个 Mock Project
        val mockProject2 = mock(Project::class.java)
        `when`(mockProject2.name).thenReturn("TestProject2")
        `when`(mockProject2.basePath).thenReturn("/test/project2")

        // 创建第二个初始化面板实例
        val initPanel2 = InitProjectPanel(mockProject2, testScope)

        // 获取两个初始化面板的内容
        val content1 = initPanel.getContent()
        val content2 = initPanel2.getContent()

        // 验证两个初始化面板的内容是不同的实例
        assertNotSame(content1, content2, "不同项目的初始化面板应该是独立的实例")
    }

    @Test
    @DisplayName("测试面板边框")
    fun testPanelBorder() {
        val content = initPanel.getContent() as JPanel

        // 验证主面板有边框（边距）
        assertNotNull(content.border, "初始化面板应该有边框")
    }

    @Test
    @DisplayName("测试面板的可见性")
    fun testPanelVisibility() {
        val content = initPanel.getContent()

        // 验证面板默认可见
        assertTrue(content.isVisible, "初始化面板应该默认可见")
    }

    /**
     * 递归查找指定类型的组件
     *
     * @param T 组件类型
     * @param container 容器组件
     * @return 找到的组件列表
     */
    private inline fun <reified T : JComponent> findComponentsOfType(container: java.awt.Container): List<T> {
        val result = mutableListOf<T>()

        return result
    }
}

