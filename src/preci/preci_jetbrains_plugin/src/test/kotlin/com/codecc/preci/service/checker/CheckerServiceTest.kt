package com.codecc.preci.service.checker

import com.codecc.preci.BaseTest
import com.codecc.preci.api.model.response.CheckerSet
import com.codecc.preci.api.model.response.CheckerSetListResponse
import com.codecc.preci.api.model.response.CheckerSetSelectResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * CheckerService 单元测试
 *
 * 测试规则集服务的数据模型和基本功能。
 *
 * **测试范围：**
 * - CheckerSetInfo 数据类
 * - CheckerSetListResult 密封类
 * - CheckerSetSelectResult 密封类
 * - CheckerSetPersistResult 密封类
 *
 * @since 1.0
 */
@DisplayName("CheckerService 测试")
class CheckerServiceTest : BaseTest() {

    // ========== CheckerSetInfo 测试 ==========

    @Nested
    @DisplayName("CheckerSetInfo 数据类测试")
    inner class CheckerSetInfoTests {

        @Test
        @DisplayName("测试 CheckerSetInfo 基本创建")
        fun testCheckerSetInfoCreation() {
            val info = CheckerSetInfo(
                id = "golang_standard",
                name = "Go 标准规则集",
                toolName = "golangci-lint"
            )

            assertEquals("golang_standard", info.id)
            assertEquals("Go 标准规则集", info.name)
            assertEquals("golangci-lint", info.toolName)
        }

        @Test
        @DisplayName("测试从 API 响应转换")
        fun testFromApiResponse() {
            val apiCheckerSet = CheckerSet(
                checkerSetId = "security_basic",
                checkerSetName = "安全基础规则集",
                toolName = "gosec"
            )

            val info = CheckerSetInfo.fromApiResponse(apiCheckerSet)

            assertEquals("security_basic", info.id)
            assertEquals("安全基础规则集", info.name)
            assertEquals("gosec", info.toolName)
        }

        @Test
        @DisplayName("测试 CheckerSetInfo 相等性")
        fun testCheckerSetInfoEquality() {
            val info1 = CheckerSetInfo("id1", "name1", "tool1")
            val info2 = CheckerSetInfo("id1", "name1", "tool1")
            val info3 = CheckerSetInfo("id2", "name2", "tool2")

            assertEquals(info1, info2)
            assertNotEquals(info1, info3)
        }

        @Test
        @DisplayName("测试 CheckerSetInfo hashCode")
        fun testCheckerSetInfoHashCode() {
            val info1 = CheckerSetInfo("id1", "name1", "tool1")
            val info2 = CheckerSetInfo("id1", "name1", "tool1")

            assertEquals(info1.hashCode(), info2.hashCode())
        }

        @Test
        @DisplayName("测试 CheckerSetInfo toString")
        fun testCheckerSetInfoToString() {
            val info = CheckerSetInfo("test_id", "Test Name", "test_tool")
            val toString = info.toString()

            assertTrue(toString.contains("test_id"))
            assertTrue(toString.contains("Test Name"))
            assertTrue(toString.contains("test_tool"))
        }

        @Test
        @DisplayName("测试 CheckerSetInfo copy")
        fun testCheckerSetInfoCopy() {
            val original = CheckerSetInfo("id1", "name1", "tool1")
            val copied = original.copy(name = "modified_name")

            assertEquals("id1", copied.id)
            assertEquals("modified_name", copied.name)
            assertEquals("tool1", copied.toolName)
        }
    }

    // ========== CheckerSetListResult 测试 ==========

    @Nested
    @DisplayName("CheckerSetListResult 密封类测试")
    inner class CheckerSetListResultTests {

        @Test
        @DisplayName("测试 CheckerSetListResult.Success")
        fun testCheckerSetListResultSuccess() {
            val checkerSets = listOf(
                CheckerSetInfo("set1", "规则集1", "tool1"),
                CheckerSetInfo("set2", "规则集2", "tool2")
            )
            val response = CheckerSetListResponse(
                checkerSets = listOf(
                    CheckerSet("set1", "规则集1", "tool1"),
                    CheckerSet("set2", "规则集2", "tool2")
                )
            )

            val result = CheckerSetListResult.Success(checkerSets, response)

            assertTrue(result is CheckerSetListResult.Success)
            assertEquals(2, result.checkerSets.size)
            assertEquals("set1", result.checkerSets[0].id)
            assertEquals("set2", result.checkerSets[1].id)
            assertEquals(response, result.response)
        }

        @Test
        @DisplayName("测试 CheckerSetListResult.Success 空列表")
        fun testCheckerSetListResultSuccessEmpty() {
            val result = CheckerSetListResult.Success(
                emptyList(),
                CheckerSetListResponse(emptyList())
            )

            assertTrue(result is CheckerSetListResult.Success)
            assertTrue(result.checkerSets.isEmpty())
        }

        @Test
        @DisplayName("测试 CheckerSetListResult.Failure")
        fun testCheckerSetListResultFailure() {
            val errorMessage = "网络错误：连接超时"
            val cause = Exception("Connection timeout")
            val result = CheckerSetListResult.Failure(errorMessage, cause)

            assertTrue(result is CheckerSetListResult.Failure)
            assertEquals(errorMessage, result.message)
            assertNotNull(result.cause)
            assertEquals("Connection timeout", result.cause?.message)
        }

        @Test
        @DisplayName("测试 CheckerSetListResult.Failure 不带 cause")
        fun testCheckerSetListResultFailureWithoutCause() {
            val result = CheckerSetListResult.Failure("获取失败")

            assertTrue(result is CheckerSetListResult.Failure)
            assertEquals("获取失败", result.message)
            assertNull(result.cause)
        }

        @Test
        @DisplayName("测试 CheckerSetListResult 类型安全")
        fun testCheckerSetListResultTypeSafety() {
            val successResult: CheckerSetListResult = CheckerSetListResult.Success(
                listOf(CheckerSetInfo("id", "name", "tool")),
                CheckerSetListResponse(listOf(CheckerSet("id", "name", "tool")))
            )
            val failureResult: CheckerSetListResult = CheckerSetListResult.Failure("error")

            // 使用 when 表达式确保类型安全
            val successMessage = when (successResult) {
                is CheckerSetListResult.Success -> "Success: ${successResult.checkerSets.size} sets"
                is CheckerSetListResult.Failure -> "Failure: ${successResult.message}"
            }
            assertTrue(successMessage.contains("Success"))

            val failureMessage = when (failureResult) {
                is CheckerSetListResult.Success -> "Success: ${failureResult.checkerSets.size} sets"
                is CheckerSetListResult.Failure -> "Failure: ${failureResult.message}"
            }
            assertTrue(failureMessage.contains("Failure"))
        }
    }

    // ========== CheckerSetSelectResult 测试 ==========

    @Nested
    @DisplayName("CheckerSetSelectResult 密封类测试")
    inner class CheckerSetSelectResultTests {

        @Test
        @DisplayName("测试 CheckerSetSelectResult.Success")
        fun testCheckerSetSelectResultSuccess() {
            val response = CheckerSetSelectResponse(
                projectRoot = "/path/to/project",
                checkerSets = listOf("set1", "set2")
            )

            val result = CheckerSetSelectResult.Success(
                projectRoot = "/path/to/project",
                selectedSets = listOf("set1", "set2"),
                response = response
            )

            assertTrue(result is CheckerSetSelectResult.Success)
            assertEquals("/path/to/project", result.projectRoot)
            assertEquals(2, result.selectedSets.size)
            assertEquals("set1", result.selectedSets[0])
            assertEquals("set2", result.selectedSets[1])
            assertEquals(response, result.response)
        }

        @Test
        @DisplayName("测试 CheckerSetSelectResult.Success 空选择")
        fun testCheckerSetSelectResultSuccessEmpty() {
            val response = CheckerSetSelectResponse(
                projectRoot = "/path/to/project",
                checkerSets = emptyList()
            )

            val result = CheckerSetSelectResult.Success(
                projectRoot = "/path/to/project",
                selectedSets = emptyList(),
                response = response
            )

            assertTrue(result is CheckerSetSelectResult.Success)
            assertTrue(result.selectedSets.isEmpty())
        }

        @Test
        @DisplayName("测试 CheckerSetSelectResult.Failure")
        fun testCheckerSetSelectResultFailure() {
            val errorMessage = "规则集不存在"
            val cause = IllegalArgumentException("Invalid checker set ID")
            val result = CheckerSetSelectResult.Failure(errorMessage, cause)

            assertTrue(result is CheckerSetSelectResult.Failure)
            assertEquals(errorMessage, result.message)
            assertNotNull(result.cause)
            assertEquals("Invalid checker set ID", result.cause?.message)
        }

        @Test
        @DisplayName("测试 CheckerSetSelectResult.Failure 不带 cause")
        fun testCheckerSetSelectResultFailureWithoutCause() {
            val result = CheckerSetSelectResult.Failure("选择失败")

            assertTrue(result is CheckerSetSelectResult.Failure)
            assertEquals("选择失败", result.message)
            assertNull(result.cause)
        }

        @Test
        @DisplayName("测试 CheckerSetSelectResult 类型安全")
        fun testCheckerSetSelectResultTypeSafety() {
            val successResult: CheckerSetSelectResult = CheckerSetSelectResult.Success(
                "/project",
                listOf("set1"),
                CheckerSetSelectResponse("/project", listOf("set1"))
            )
            val failureResult: CheckerSetSelectResult = CheckerSetSelectResult.Failure("error")

            // 使用 when 表达式确保类型安全
            val successMessage = when (successResult) {
                is CheckerSetSelectResult.Success -> "Success: ${successResult.selectedSets.size} selected"
                is CheckerSetSelectResult.Failure -> "Failure: ${successResult.message}"
            }
            assertTrue(successMessage.contains("Success"))

            val failureMessage = when (failureResult) {
                is CheckerSetSelectResult.Success -> "Success: ${failureResult.selectedSets.size} selected"
                is CheckerSetSelectResult.Failure -> "Failure: ${failureResult.message}"
            }
            assertTrue(failureMessage.contains("Failure"))
        }
    }

    // ========== CheckerSetPersistResult 测试 ==========

    @Nested
    @DisplayName("CheckerSetPersistResult 密封类测试")
    inner class CheckerSetPersistResultTests {

        @Test
        @DisplayName("测试 CheckerSetPersistResult.Success")
        fun testCheckerSetPersistResultSuccess() {
            val savedSets = listOf("set1", "set2", "set3")
            val result = CheckerSetPersistResult.Success(savedSets)

            assertTrue(result is CheckerSetPersistResult.Success)
            assertEquals(3, result.savedSets.size)
            assertEquals("set1", result.savedSets[0])
            assertEquals("set2", result.savedSets[1])
            assertEquals("set3", result.savedSets[2])
        }

        @Test
        @DisplayName("测试 CheckerSetPersistResult.Success 空列表")
        fun testCheckerSetPersistResultSuccessEmpty() {
            val result = CheckerSetPersistResult.Success(emptyList())

            assertTrue(result is CheckerSetPersistResult.Success)
            assertTrue(result.savedSets.isEmpty())
        }

        @Test
        @DisplayName("测试 CheckerSetPersistResult.Failure")
        fun testCheckerSetPersistResultFailure() {
            val errorMessage = "配置文件写入失败"
            val cause = java.io.IOException("Permission denied")
            val result = CheckerSetPersistResult.Failure(errorMessage, cause)

            assertTrue(result is CheckerSetPersistResult.Failure)
            assertEquals(errorMessage, result.message)
            assertNotNull(result.cause)
            assertEquals("Permission denied", result.cause?.message)
        }

        @Test
        @DisplayName("测试 CheckerSetPersistResult.Failure 不带 cause")
        fun testCheckerSetPersistResultFailureWithoutCause() {
            val result = CheckerSetPersistResult.Failure("持久化失败")

            assertTrue(result is CheckerSetPersistResult.Failure)
            assertEquals("持久化失败", result.message)
            assertNull(result.cause)
        }

        @Test
        @DisplayName("测试 CheckerSetPersistResult 类型安全")
        fun testCheckerSetPersistResultTypeSafety() {
            val successResult: CheckerSetPersistResult = CheckerSetPersistResult.Success(listOf("set1"))
            val failureResult: CheckerSetPersistResult = CheckerSetPersistResult.Failure("error")

            // 使用 when 表达式确保类型安全
            val successMessage = when (successResult) {
                is CheckerSetPersistResult.Success -> "Success: ${successResult.savedSets.size} saved"
                is CheckerSetPersistResult.Failure -> "Failure: ${successResult.message}"
            }
            assertTrue(successMessage.contains("Success"))

            val failureMessage = when (failureResult) {
                is CheckerSetPersistResult.Success -> "Success: ${failureResult.savedSets.size} saved"
                is CheckerSetPersistResult.Failure -> "Failure: ${failureResult.message}"
            }
            assertTrue(failureMessage.contains("Failure"))
        }
    }

    // ========== 数据类功能测试 ==========

    @Nested
    @DisplayName("数据类功能测试")
    inner class DataClassFunctionalityTests {

        @Test
        @DisplayName("测试 CheckerSetListResult.Success 数据类功能")
        fun testCheckerSetListResultSuccessDataClass() {
            val checkerSets = listOf(CheckerSetInfo("set1", "规则集1", "tool1"))
            val response = CheckerSetListResponse(
                listOf(CheckerSet("set1", "规则集1", "tool1"))
            )

            val result1 = CheckerSetListResult.Success(checkerSets, response)
            val result2 = CheckerSetListResult.Success(checkerSets, response)

            // 测试相等性
            assertEquals(result1, result2)

            // 测试 toString
            val toString = result1.toString()
            assertTrue(toString.contains("Success"))
            assertTrue(toString.contains("set1"))
        }

        @Test
        @DisplayName("测试 CheckerSetSelectResult.Success 数据类功能")
        fun testCheckerSetSelectResultSuccessDataClass() {
            val response = CheckerSetSelectResponse("/project", listOf("set1"))

            val result1 = CheckerSetSelectResult.Success("/project", listOf("set1"), response)
            val result2 = CheckerSetSelectResult.Success("/project", listOf("set1"), response)

            // 测试相等性
            assertEquals(result1, result2)

            // 测试 toString
            val toString = result1.toString()
            assertTrue(toString.contains("Success"))
            assertTrue(toString.contains("/project"))
        }

        @Test
        @DisplayName("测试 CheckerSetPersistResult.Success 数据类功能")
        fun testCheckerSetPersistResultSuccessDataClass() {
            val result1 = CheckerSetPersistResult.Success(listOf("set1", "set2"))
            val result2 = CheckerSetPersistResult.Success(listOf("set1", "set2"))

            // 测试相等性
            assertEquals(result1, result2)

            // 测试 toString
            val toString = result1.toString()
            assertTrue(toString.contains("Success"))
            assertTrue(toString.contains("set1"))
        }

        @Test
        @DisplayName("测试 Failure 数据类功能")
        fun testFailureDataClassFunctionality() {
            val cause = Exception("test error")

            val listFailure1 = CheckerSetListResult.Failure("error1", cause)
            val listFailure2 = CheckerSetListResult.Failure("error1", cause)
            assertEquals(listFailure1, listFailure2)

            val selectFailure1 = CheckerSetSelectResult.Failure("error2", cause)
            val selectFailure2 = CheckerSetSelectResult.Failure("error2", cause)
            assertEquals(selectFailure1, selectFailure2)

            val persistFailure1 = CheckerSetPersistResult.Failure("error3", cause)
            val persistFailure2 = CheckerSetPersistResult.Failure("error3", cause)
            assertEquals(persistFailure1, persistFailure2)
        }
    }

    // ========== CheckerSetSelectResult (unselect 场景) 测试 ==========

    @Nested
    @DisplayName("CheckerSetSelectResult (unselect 场景) 测试")
    inner class CheckerSetUnselectResultTests {

        @Test
        @DisplayName("测试 unselect 成功 - 带 response")
        fun testUnselectSuccessWithResponse() {
            val response = CheckerSetSelectResponse(
                projectRoot = "/path/to/project",
                checkerSets = listOf("set1")
            )

            val result = CheckerSetSelectResult.Success(
                projectRoot = "/path/to/project",
                selectedSets = listOf("set1"),
                response = response
            )

            assertTrue(result is CheckerSetSelectResult.Success)
            assertEquals("/path/to/project", result.projectRoot)
            assertEquals(1, result.selectedSets.size)
            assertNotNull(result.response)
        }

        @Test
        @DisplayName("测试 unselect 成功 - 不带 response")
        fun testUnselectSuccessWithoutResponse() {
            val result = CheckerSetSelectResult.Success(
                projectRoot = "/path/to/project",
                selectedSets = emptyList()
            )

            assertTrue(result is CheckerSetSelectResult.Success)
            assertEquals("/path/to/project", result.projectRoot)
            assertTrue(result.selectedSets.isEmpty())
            assertNull(result.response)
        }

        @Test
        @DisplayName("测试计算取消勾选的差集逻辑")
        fun testComputeUnselectedDiff() {
            val initialCheckerSets = setOf("set1", "set2", "set3")
            val selectedCheckerSetIds = listOf("set1", "set3")

            val unselectedIds = initialCheckerSets.filter { it !in selectedCheckerSetIds }

            assertEquals(1, unselectedIds.size)
            assertEquals("set2", unselectedIds[0])
        }

        @Test
        @DisplayName("测试差集 - 全部取消勾选")
        fun testComputeUnselectedDiffAllRemoved() {
            val initialCheckerSets = setOf("set1", "set2")
            val selectedCheckerSetIds = emptyList<String>()

            val unselectedIds = initialCheckerSets.filter { it !in selectedCheckerSetIds }

            assertEquals(2, unselectedIds.size)
            assertTrue(unselectedIds.containsAll(listOf("set1", "set2")))
        }

        @Test
        @DisplayName("测试差集 - 无变化")
        fun testComputeUnselectedDiffNoChange() {
            val initialCheckerSets = setOf("set1", "set2")
            val selectedCheckerSetIds = listOf("set1", "set2")

            val unselectedIds = initialCheckerSets.filter { it !in selectedCheckerSetIds }

            assertTrue(unselectedIds.isEmpty())
        }

        @Test
        @DisplayName("测试差集 - 初始为空")
        fun testComputeUnselectedDiffInitialEmpty() {
            val initialCheckerSets = emptySet<String>()
            val selectedCheckerSetIds = listOf("set1")

            val unselectedIds = initialCheckerSets.filter { it !in selectedCheckerSetIds }

            assertTrue(unselectedIds.isEmpty())
        }

        @Test
        @DisplayName("测试差集 - 新增且删除")
        fun testComputeUnselectedDiffAddAndRemove() {
            val initialCheckerSets = setOf("set1", "set2")
            val selectedCheckerSetIds = listOf("set2", "set3")

            val unselectedIds = initialCheckerSets.filter { it !in selectedCheckerSetIds }

            assertEquals(1, unselectedIds.size)
            assertEquals("set1", unselectedIds[0])
        }
    }

    // ========== 边界情况测试 ==========

    @Nested
    @DisplayName("边界情况测试")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("测试空字符串 ID")
        fun testEmptyStringId() {
            val info = CheckerSetInfo("", "name", "tool")
            assertEquals("", info.id)
        }

        @Test
        @DisplayName("测试特殊字符")
        fun testSpecialCharacters() {
            val info = CheckerSetInfo(
                id = "set-with_special.chars",
                name = "规则集<名称>",
                toolName = "tool/name"
            )

            assertEquals("set-with_special.chars", info.id)
            assertEquals("规则集<名称>", info.name)
            assertEquals("tool/name", info.toolName)
        }

        @Test
        @DisplayName("测试长字符串")
        fun testLongStrings() {
            val longId = "a".repeat(1000)
            val longName = "名".repeat(500)
            val longToolName = "t".repeat(800)

            val info = CheckerSetInfo(longId, longName, longToolName)

            assertEquals(1000, info.id.length)
            assertEquals(500, info.name.length)
            assertEquals(800, info.toolName.length)
        }

        @Test
        @DisplayName("测试 Unicode 字符")
        fun testUnicodeCharacters() {
            val info = CheckerSetInfo(
                id = "规则集ID_🎉",
                name = "Go 规则集 🚀",
                toolName = "工具名称 ☕"
            )

            assertTrue(info.id.contains("🎉"))
            assertTrue(info.name.contains("🚀"))
            assertTrue(info.toolName.contains("☕"))
        }

        @Test
        @DisplayName("测试大量规则集")
        fun testLargeNumberOfSets() {
            val checkerSets = (1..1000).map { index ->
                CheckerSetInfo("set_$index", "规则集 $index", "tool_${index % 10}")
            }

            assertEquals(1000, checkerSets.size)
            assertEquals("set_1", checkerSets.first().id)
            assertEquals("set_1000", checkerSets.last().id)
        }
    }

    /*
     * 以下是集成测试，需要 PreCI Local Server 运行和 IntelliJ 平台环境
     * 手动测试时可以取消注释
     *
    @Test
    @DisplayName("测试获取规则集列表")
    fun testGetCheckerSetList() = runBlocking {
        val project = createTestProject()
        val checkerService = CheckerService.getInstance(project)

        when (val result = checkerService.getCheckerSetList()) {
            is CheckerSetListResult.Success -> {
                println("获取成功，共 ${result.checkerSets.size} 个规则集:")
                result.checkerSets.forEach { set ->
                    println("  - ${set.id}: ${set.name} (${set.toolName})")
                }
            }
            is CheckerSetListResult.Failure -> {
                println("获取失败: ${result.message}")
            }
        }
    }

    @Test
    @DisplayName("测试选择规则集")
    fun testSelectCheckerSets() = runBlocking {
        val project = createTestProject()
        val checkerService = CheckerService.getInstance(project)

        when (val result = checkerService.selectCheckerSets(listOf("golang_standard", "security_basic"))) {
            is CheckerSetSelectResult.Success -> {
                println("选择成功:")
                println("  项目: ${result.projectRoot}")
                println("  已选择: ${result.selectedSets}")
            }
            is CheckerSetSelectResult.Failure -> {
                println("选择失败: ${result.message}")
            }
        }
    }

    @Test
    @DisplayName("测试配置持久化")
    fun testPersistence() {
        val project = createTestProject()
        val checkerService = CheckerService.getInstance(project)

        // 保存配置
        val saveResult = checkerService.saveSelectedCheckerSets(listOf("set1", "set2"))
        when (saveResult) {
            is CheckerSetPersistResult.Success -> {
                println("保存成功: ${saveResult.savedSets}")
            }
            is CheckerSetPersistResult.Failure -> {
                println("保存失败: ${saveResult.message}")
            }
        }

        // 加载配置
        val loadedSets = checkerService.loadSelectedCheckerSets()
        println("加载的配置: $loadedSets")

        // 清除配置
        val clearResult = checkerService.clearSelectedCheckerSets()
        when (clearResult) {
            is CheckerSetPersistResult.Success -> {
                println("清除成功")
            }
            is CheckerSetPersistResult.Failure -> {
                println("清除失败: ${clearResult.message}")
            }
        }
    }
    */
}
