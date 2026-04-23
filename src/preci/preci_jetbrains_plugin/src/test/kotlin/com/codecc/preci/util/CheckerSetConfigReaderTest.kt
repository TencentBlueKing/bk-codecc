package com.codecc.preci.util

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * CheckerSetConfigReader 单元测试
 *
 * 测试规则集配置读取工具类的各种场景，包括正常读取、空目录、错误处理等。
 *
 * @since 1.0
 */
class CheckerSetConfigReaderTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var projectRoot: File
    private lateinit var codeccDir: File
    private lateinit var checkerSetDir: File

    @BeforeEach
    fun setUp() {
        // 创建测试目录结构
        projectRoot = tempDir.resolve("test-project")
        projectRoot.mkdirs()

        codeccDir = projectRoot.resolve(".codecc")
        checkerSetDir = codeccDir.resolve("checkerset")
    }

    @AfterEach
    fun tearDown() {
        // 清理临时文件
        projectRoot.deleteRecursively()
    }

    /**
     * 测试：成功读取规则集配置
     *
     * **场景：** `.codecc/checkerset/` 目录包含多个 JSON 文件
     * **预期：** 返回所有 JSON 文件的名称（不包含后缀）
     */
    @Test
    fun `test getSelectedCheckerSets - success`() {
        // 创建目录和文件
        checkerSetDir.mkdirs()
        checkerSetDir.resolve("golang_standard.json").writeText("{}")
        checkerSetDir.resolve("security_basic.json").writeText("{}")
        checkerSetDir.resolve("performance.json").writeText("{}")

        // 调用方法
        val result = CheckerSetConfigReader.getSelectedCheckerSets(projectRoot.absolutePath)

        // 验证结果
        assertEquals(3, result.size)
        assertTrue(result.contains("golang_standard"))
        assertTrue(result.contains("performance"))
        assertTrue(result.contains("security_basic"))
        
        // 验证结果已排序
        assertEquals(listOf("golang_standard", "performance", "security_basic"), result)
    }

    /**
     * 测试：项目根目录不存在
     *
     * **场景：** 提供的项目根目录路径不存在
     * **预期：** 返回空列表
     */
    @Test
    fun `test getSelectedCheckerSets - project root not exists`() {
        val nonExistentPath = tempDir.resolve("non-existent").absolutePath
        val result = CheckerSetConfigReader.getSelectedCheckerSets(nonExistentPath)

        assertEquals(emptyList<String>(), result)
    }

    /**
     * 测试：项目根目录是文件而不是目录
     *
     * **场景：** 提供的项目根目录路径是一个文件
     * **预期：** 返回空列表
     */
    @Test
    fun `test getSelectedCheckerSets - project root is file`() {
        val file = tempDir.resolve("file.txt")
        file.writeText("test")

        val result = CheckerSetConfigReader.getSelectedCheckerSets(file.absolutePath)

        assertEquals(emptyList<String>(), result)
    }

    /**
     * 测试：.codecc/checkerset 目录不存在
     *
     * **场景：** 项目根目录存在但没有 `.codecc/checkerset/` 目录
     * **预期：** 返回空列表
     */
    @Test
    fun `test getSelectedCheckerSets - checkerset dir not exists`() {
        // 项目根目录存在，但没有创建 .codecc/checkerset
        val result = CheckerSetConfigReader.getSelectedCheckerSets(projectRoot.absolutePath)

        assertEquals(emptyList<String>(), result)
    }

    /**
     * 测试：checkerset 目录为空
     *
     * **场景：** `.codecc/checkerset/` 目录存在但为空
     * **预期：** 返回空列表
     */
    @Test
    fun `test getSelectedCheckerSets - checkerset dir is empty`() {
        checkerSetDir.mkdirs()

        val result = CheckerSetConfigReader.getSelectedCheckerSets(projectRoot.absolutePath)

        assertEquals(emptyList<String>(), result)
    }

    /**
     * 测试：checkerset 目录只包含非 JSON 文件
     *
     * **场景：** `.codecc/checkerset/` 目录包含 `.txt` 等非 JSON 文件
     * **预期：** 返回空列表（忽略非 JSON 文件）
     */
    @Test
    fun `test getSelectedCheckerSets - only non-json files`() {
        checkerSetDir.mkdirs()
        checkerSetDir.resolve("readme.txt").writeText("test")
        checkerSetDir.resolve("config.xml").writeText("<xml/>")

        val result = CheckerSetConfigReader.getSelectedCheckerSets(projectRoot.absolutePath)

        assertEquals(emptyList<String>(), result)
    }

    /**
     * 测试：checkerset 目录包含混合文件
     *
     * **场景：** `.codecc/checkerset/` 目录包含 JSON 文件和其他文件
     * **预期：** 只返回 JSON 文件的名称
     */
    @Test
    fun `test getSelectedCheckerSets - mixed files`() {
        checkerSetDir.mkdirs()
        checkerSetDir.resolve("golang_standard.json").writeText("{}")
        checkerSetDir.resolve("readme.txt").writeText("test")
        checkerSetDir.resolve("security_basic.json").writeText("{}")
        checkerSetDir.resolve("config.xml").writeText("<xml/>")

        val result = CheckerSetConfigReader.getSelectedCheckerSets(projectRoot.absolutePath)

        assertEquals(2, result.size)
        assertTrue(result.contains("golang_standard"))
        assertTrue(result.contains("security_basic"))
        assertFalse(result.contains("readme"))
        assertFalse(result.contains("config"))
    }

    /**
     * 测试：checkerset 目录包含子目录
     *
     * **场景：** `.codecc/checkerset/` 目录包含子目录
     * **预期：** 忽略子目录，只返回 JSON 文件
     */
    @Test
    fun `test getSelectedCheckerSets - with subdirectories`() {
        checkerSetDir.mkdirs()
        checkerSetDir.resolve("golang_standard.json").writeText("{}")
        
        // 创建子目录
        val subDir = checkerSetDir.resolve("backup")
        subDir.mkdirs()
        subDir.resolve("old.json").writeText("{}")

        val result = CheckerSetConfigReader.getSelectedCheckerSets(projectRoot.absolutePath)

        assertEquals(1, result.size)
        assertEquals("golang_standard", result[0])
    }

    /**
     * 测试：hasCheckerSetConfig - 有配置
     *
     * **场景：** 项目已配置规则集
     * **预期：** 返回 true
     */
    @Test
    fun `test hasCheckerSetConfig - has config`() {
        checkerSetDir.mkdirs()
        checkerSetDir.resolve("golang_standard.json").writeText("{}")

        val result = CheckerSetConfigReader.hasCheckerSetConfig(projectRoot.absolutePath)

        assertTrue(result)
    }

    /**
     * 测试：hasCheckerSetConfig - 无配置
     *
     * **场景：** 项目未配置规则集
     * **预期：** 返回 false
     */
    @Test
    fun `test hasCheckerSetConfig - no config`() {
        val result = CheckerSetConfigReader.hasCheckerSetConfig(projectRoot.absolutePath)

        assertFalse(result)
    }

    /**
     * 测试：getCheckerSetConfigDir
     *
     * **场景：** 获取规则集配置目录路径
     * **预期：** 返回 `.codecc/checkerset/` 的绝对路径
     */
    @Test
    fun `test getCheckerSetConfigDir`() {
        val result = CheckerSetConfigReader.getCheckerSetConfigDir(projectRoot.absolutePath)

        val expected = checkerSetDir.absolutePath
        assertEquals(expected, result)
    }

    /**
     * 测试：JSON 文件名包含特殊字符
     *
     * **场景：** JSON 文件名包含下划线、连字符等
     * **预期：** 正确读取文件名
     */
    @Test
    fun `test getSelectedCheckerSets - special characters in filename`() {
        checkerSetDir.mkdirs()
        checkerSetDir.resolve("golang-standard_v1.json").writeText("{}")
        checkerSetDir.resolve("security_basic-2.0.json").writeText("{}")

        val result = CheckerSetConfigReader.getSelectedCheckerSets(projectRoot.absolutePath)

        assertEquals(2, result.size)
        assertTrue(result.contains("golang-standard_v1"))
        assertTrue(result.contains("security_basic-2.0"))
    }

    /**
     * 测试：大小写敏感性
     *
     * **场景：** JSON 文件名包含大写字母
     * **预期：** 保留原始大小写
     */
    @Test
    fun `test getSelectedCheckerSets - case sensitivity`() {
        checkerSetDir.mkdirs()
        checkerSetDir.resolve("GoLang_Standard.json").writeText("{}")
        checkerSetDir.resolve("SECURITY_BASIC.json").writeText("{}")

        val result = CheckerSetConfigReader.getSelectedCheckerSets(projectRoot.absolutePath)

        assertEquals(2, result.size)
        // 结果已排序，大写字母排在前面
        assertTrue(result.contains("GoLang_Standard"))
        assertTrue(result.contains("SECURITY_BASIC"))
    }
}

