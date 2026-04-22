package com.codecc.preci.service.codecc

import com.codecc.preci.BaseTest
import com.codecc.preci.api.model.response.RemoteDefect
import com.codecc.preci.api.model.response.RemoteDefectListResponse
import com.codecc.preci.api.model.response.RemoteTaskInfo
import com.codecc.preci.api.model.response.RemoteTaskListResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * CodeCCService 单元测试
 *
 * 测试 CodeCC 远程代码检查服务的数据模型和基本功能。
 *
 * **测试范围：**
 * - RemoteTaskListResult 密封类
 * - RemoteDefectListResult 密封类
 * - 远程缺陷到本地缺陷的转换逻辑
 * - 边界情况处理
 *
 * @since 1.0
 */
@DisplayName("CodeCCService 测试")
class CodeCCServiceTest : BaseTest() {

    // ========== RemoteTaskListResult 测试 ==========

    @Nested
    @DisplayName("RemoteTaskListResult 密封类测试")
    inner class RemoteTaskListResultTests {

        @Test
        @DisplayName("测试 Success 正常场景")
        fun testSuccess() {
            val tasks = listOf(
                RemoteTaskInfo(100001, "task1", "任务1"),
                RemoteTaskInfo(100002, "task2", "任务2")
            )
            val response = RemoteTaskListResponse(tasks)

            val result = RemoteTaskListResult.Success(tasks, response)

            assertTrue(result is RemoteTaskListResult.Success)
            assertEquals(2, result.tasks.size)
            assertEquals(100001L, result.tasks[0].taskId)
            assertEquals(response, result.response)
        }

        @Test
        @DisplayName("测试 Success 空列表")
        fun testSuccessEmpty() {
            val result = RemoteTaskListResult.Success(
                emptyList(),
                RemoteTaskListResponse(emptyList())
            )

            assertTrue(result is RemoteTaskListResult.Success)
            assertTrue(result.tasks.isEmpty())
        }

        @Test
        @DisplayName("测试 Failure")
        fun testFailure() {
            val cause = Exception("Network timeout")
            val result = RemoteTaskListResult.Failure("网络错误：连接超时", cause)

            assertTrue(result is RemoteTaskListResult.Failure)
            assertEquals("网络错误：连接超时", result.message)
            assertNotNull(result.cause)
            assertEquals("Network timeout", result.cause?.message)
        }

        @Test
        @DisplayName("测试 Failure 不带 cause")
        fun testFailureWithoutCause() {
            val result = RemoteTaskListResult.Failure("获取失败")

            assertTrue(result is RemoteTaskListResult.Failure)
            assertEquals("获取失败", result.message)
            assertNull(result.cause)
        }

        @Test
        @DisplayName("测试类型安全")
        fun testTypeSafety() {
            val successResult: RemoteTaskListResult = RemoteTaskListResult.Success(
                listOf(RemoteTaskInfo(1, "a", "A")),
                RemoteTaskListResponse(listOf(RemoteTaskInfo(1, "a", "A")))
            )
            val failureResult: RemoteTaskListResult = RemoteTaskListResult.Failure("error")

            val successMessage = when (successResult) {
                is RemoteTaskListResult.Success -> "成功: ${successResult.tasks.size} 个任务"
                is RemoteTaskListResult.Failure -> "失败: ${successResult.message}"
            }
            assertTrue(successMessage.contains("成功"))

            val failureMessage = when (failureResult) {
                is RemoteTaskListResult.Success -> "成功: ${failureResult.tasks.size} 个任务"
                is RemoteTaskListResult.Failure -> "失败: ${failureResult.message}"
            }
            assertTrue(failureMessage.contains("失败"))
        }

        @Test
        @DisplayName("测试相等性")
        fun testEquality() {
            val tasks = listOf(RemoteTaskInfo(1, "a", "A"))
            val response = RemoteTaskListResponse(tasks)
            val result1 = RemoteTaskListResult.Success(tasks, response)
            val result2 = RemoteTaskListResult.Success(tasks, response)

            assertEquals(result1, result2)
        }
    }

    // ========== RemoteDefectListResult 测试 ==========

    @Nested
    @DisplayName("RemoteDefectListResult 密封类测试")
    inner class RemoteDefectListResultTests {

        @Test
        @DisplayName("测试 Success")
        fun testSuccess() {
            val response = RemoteDefectListResponse(
                seriousCount = 5,
                normalCount = 10,
                promptCount = 3,
                totalCount = 18,
                defects = listOf(
                    RemoteDefect(fileName = "a.go", severity = 1),
                    RemoteDefect(fileName = "b.go", severity = 2)
                )
            )

            val result = RemoteDefectListResult.Success(response)

            assertTrue(result is RemoteDefectListResult.Success)
            assertEquals(18, result.response.totalCount)
            assertEquals(2, result.response.getDefectList().size)
        }

        @Test
        @DisplayName("测试 Success 空缺陷")
        fun testSuccessEmpty() {
            val response = RemoteDefectListResponse(totalCount = 0)
            val result = RemoteDefectListResult.Success(response)

            assertTrue(result is RemoteDefectListResult.Success)
            assertEquals(0, result.response.totalCount)
            assertTrue(result.response.getDefectList().isEmpty())
        }

        @Test
        @DisplayName("测试 Failure")
        fun testFailure() {
            val cause = Exception("Auth failed")
            val result = RemoteDefectListResult.Failure("认证失败", cause)

            assertTrue(result is RemoteDefectListResult.Failure)
            assertEquals("认证失败", result.message)
            assertNotNull(result.cause)
        }

        @Test
        @DisplayName("测试 Failure 不带 cause")
        fun testFailureWithoutCause() {
            val result = RemoteDefectListResult.Failure("查询失败")

            assertTrue(result is RemoteDefectListResult.Failure)
            assertEquals("查询失败", result.message)
            assertNull(result.cause)
        }

        @Test
        @DisplayName("测试类型安全")
        fun testTypeSafety() {
            val successResult: RemoteDefectListResult = RemoteDefectListResult.Success(
                RemoteDefectListResponse(totalCount = 5)
            )
            val failureResult: RemoteDefectListResult = RemoteDefectListResult.Failure("error")

            val successMessage = when (successResult) {
                is RemoteDefectListResult.Success -> "成功: ${successResult.response.totalCount} 条"
                is RemoteDefectListResult.Failure -> "失败: ${successResult.message}"
            }
            assertTrue(successMessage.contains("成功"))

            val failureMessage = when (failureResult) {
                is RemoteDefectListResult.Success -> "成功: ${failureResult.response.totalCount} 条"
                is RemoteDefectListResult.Failure -> "失败: ${failureResult.message}"
            }
            assertTrue(failureMessage.contains("失败"))
        }
    }

    // ========== 缺陷转换逻辑测试 ==========

    @Nested
    @DisplayName("缺陷转换逻辑测试")
    inner class DefectConversionTests {

        @Test
        @DisplayName("测试远程缺陷转本地缺陷 - 严重")
        fun testConversionSevere() {
            val remote = RemoteDefect(
                fileName = "main.go",
                filePath = "/project/main.go",
                lineNum = 42,
                author = listOf("user1"),
                checker = "errcheck",
                severity = 1,
                message = "Error not checked",
                status = 1,
                toolName = "golangci-lint"
            )

            val local = remote.toLocalDefect()

            assertEquals("golangci-lint", local.toolName)
            assertEquals("errcheck", local.checkerName)
            assertEquals("Error not checked", local.description)
            assertEquals("/project/main.go", local.filePath)
            assertEquals(42, local.line)
            assertEquals(1L, local.severity)
        }

        @Test
        @DisplayName("测试远程缺陷转本地缺陷 - 一般")
        fun testConversionNormal() {
            val remote = RemoteDefect(severity = 2, message = "style issue")
            val local = remote.toLocalDefect()
            assertEquals(2L, local.severity)
        }

        @Test
        @DisplayName("测试远程缺陷转本地缺陷 - 提示")
        fun testConversionPrompt() {
            val remote = RemoteDefect(severity = 4, message = "info")
            val local = remote.toLocalDefect()
            assertEquals(4L, local.severity)
        }

        @Test
        @DisplayName("测试远程缺陷转本地缺陷 - 建议 (severity=8)")
        fun testConversionSuggestion() {
            val remote = RemoteDefect(severity = 8, message = "suggestion")
            val local = remote.toLocalDefect()
            assertEquals(8L, local.severity)
        }

        @Test
        @DisplayName("测试批量转换")
        fun testBatchConversion() {
            val remoteDefects = listOf(
                RemoteDefect(fileName = "a.go", severity = 1, checker = "check1", toolName = "tool1"),
                RemoteDefect(fileName = "b.go", severity = 2, checker = "check2", toolName = "tool2"),
                RemoteDefect(fileName = "c.go", severity = 4, checker = "check3", toolName = "tool3")
            )

            val localDefects = remoteDefects.map { it.toLocalDefect() }

            assertEquals(3, localDefects.size)
            assertEquals(1L, localDefects[0].severity)
            assertEquals(2L, localDefects[1].severity)
            assertEquals(4L, localDefects[2].severity)
        }

        @Test
        @DisplayName("测试空缺陷列表转换")
        fun testEmptyConversion() {
            val response = RemoteDefectListResponse(defects = emptyList())
            val localDefects = response.getDefectList().map { it.toLocalDefect() }
            assertTrue(localDefects.isEmpty())
        }
    }

    // ========== 边界情况测试 ==========

    @Nested
    @DisplayName("边界情况测试")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("测试空字符串字段")
        fun testEmptyStringFields() {
            val defect = RemoteDefect(
                fileName = "",
                filePath = "",
                checker = "",
                message = "",
                toolName = ""
            )
            val local = defect.toLocalDefect()

            assertEquals("", local.toolName)
            assertEquals("", local.checkerName)
            assertEquals("", local.description)
            assertEquals("", local.filePath)
        }

        @Test
        @DisplayName("测试零行号")
        fun testZeroLineNum() {
            val defect = RemoteDefect(lineNum = 0)
            val local = defect.toLocalDefect()
            assertEquals(0, local.line)
        }

        @Test
        @DisplayName("测试负行号")
        fun testNegativeLineNum() {
            val defect = RemoteDefect(lineNum = -1)
            val local = defect.toLocalDefect()
            assertEquals(-1, local.line)
        }

        @Test
        @DisplayName("测试大量缺陷")
        fun testLargeDefectList() {
            val defects = (1..1000).map { index ->
                RemoteDefect(
                    fileName = "file_$index.go",
                    filePath = "/project/file_$index.go",
                    lineNum = index,
                    severity = listOf(1, 2, 4)[index % 3],
                    checker = "checker_$index",
                    toolName = "tool_${index % 5}"
                )
            }

            val response = RemoteDefectListResponse(
                totalCount = 1000,
                defects = defects
            )

            assertEquals(1000, response.getDefectList().size)
            val localDefects = response.getDefectList().map { it.toLocalDefect() }
            assertEquals(1000, localDefects.size)
        }

        @Test
        @DisplayName("测试 Unicode 字符")
        fun testUnicodeCharacters() {
            val defect = RemoteDefect(
                fileName = "中文文件名.go",
                message = "错误描述 🚀",
                checker = "检查规则_🎉"
            )

            assertEquals("中文文件名.go", defect.fileName)
            assertTrue(defect.message.contains("🚀"))
            assertTrue(defect.checker.contains("🎉"))
        }

        @Test
        @DisplayName("测试多责任人")
        fun testMultipleAuthors() {
            val defect = RemoteDefect(
                author = listOf("user1", "user2", "user3")
            )

            assertEquals("user1, user2, user3", defect.getAuthorText())
        }

        @Test
        @DisplayName("测试未知 severity 值")
        fun testUnknownSeverity() {
            assertEquals("未知", RemoteDefect(severity = 0).getSeverityText())
            assertEquals("未知", RemoteDefect(severity = -1).getSeverityText())
            assertEquals("未知", RemoteDefect(severity = 999).getSeverityText())
        }

        @Test
        @DisplayName("测试未知 status 值")
        fun testUnknownStatus() {
            assertEquals("未知", RemoteDefect(status = 0).getStatusText())
            assertEquals("未知", RemoteDefect(status = -1).getStatusText())
            assertEquals("未知", RemoteDefect(status = 999).getStatusText())
        }
    }

    // ========== 数据类功能测试 ==========

    @Nested
    @DisplayName("数据类功能测试")
    inner class DataClassFunctionalityTests {

        @Test
        @DisplayName("测试 RemoteTaskListResult.Success 数据类")
        fun testRemoteTaskListResultSuccessDataClass() {
            val tasks = listOf(RemoteTaskInfo(1, "a", "A"))
            val response = RemoteTaskListResponse(tasks)

            val result1 = RemoteTaskListResult.Success(tasks, response)
            val result2 = RemoteTaskListResult.Success(tasks, response)

            assertEquals(result1, result2)
            assertEquals(result1.hashCode(), result2.hashCode())
            assertTrue(result1.toString().contains("Success"))
        }

        @Test
        @DisplayName("测试 RemoteDefectListResult.Success 数据类")
        fun testRemoteDefectListResultSuccessDataClass() {
            val response = RemoteDefectListResponse(totalCount = 5)

            val result1 = RemoteDefectListResult.Success(response)
            val result2 = RemoteDefectListResult.Success(response)

            assertEquals(result1, result2)
            assertEquals(result1.hashCode(), result2.hashCode())
        }

        @Test
        @DisplayName("测试 Failure 数据类")
        fun testFailureDataClass() {
            val cause = Exception("test error")

            val taskFailure1 = RemoteTaskListResult.Failure("error", cause)
            val taskFailure2 = RemoteTaskListResult.Failure("error", cause)
            assertEquals(taskFailure1, taskFailure2)

            val defectFailure1 = RemoteDefectListResult.Failure("error", cause)
            val defectFailure2 = RemoteDefectListResult.Failure("error", cause)
            assertEquals(defectFailure1, defectFailure2)
        }
    }
}
