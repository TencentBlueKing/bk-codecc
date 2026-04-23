package com.codecc.preci.api.model

import com.codecc.preci.BaseTest
import com.codecc.preci.api.model.request.RemoteDefectListRequest
import com.codecc.preci.api.model.response.RemoteDefect
import com.codecc.preci.api.model.response.RemoteDefectListResponse
import com.codecc.preci.api.model.response.RemoteTaskInfo
import com.codecc.preci.api.model.response.RemoteTaskListResponse
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * 远程 CodeCC 模型单元测试
 *
 * 测试 API 模型的创建、序列化/反序列化和辅助方法。
 *
 * @since 1.0
 */
@DisplayName("远程 CodeCC 模型测试")
class RemoteModelsTest : BaseTest() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // ========== RemoteTaskInfo 测试 ==========

    @Nested
    @DisplayName("RemoteTaskInfo 测试")
    inner class RemoteTaskInfoTests {

        @Test
        @DisplayName("测试基本创建")
        fun testCreation() {
            val task = RemoteTaskInfo(
                taskId = 100001,
                nameEn = "preci_server_scan",
                nameCn = "PreCI 服务端扫描任务"
            )

            assertEquals(100001L, task.taskId)
            assertEquals("preci_server_scan", task.nameEn)
            assertEquals("PreCI 服务端扫描任务", task.nameCn)
        }

        @Test
        @DisplayName("测试 getDisplayName 中文名非空")
        fun testGetDisplayNameWithChinese() {
            val task = RemoteTaskInfo(100001, "scan_task", "扫描任务")
            assertEquals("扫描任务", task.getDisplayName())
        }

        @Test
        @DisplayName("测试 getDisplayName 中文名为空")
        fun testGetDisplayNameWithoutChinese() {
            val task = RemoteTaskInfo(100001, "scan_task", "")
            assertEquals("scan_task", task.getDisplayName())
        }

        @Test
        @DisplayName("测试 getDisplayName 中文名为空白")
        fun testGetDisplayNameWithBlankChinese() {
            val task = RemoteTaskInfo(100001, "scan_task", "   ")
            assertEquals("scan_task", task.getDisplayName())
        }

        @Test
        @DisplayName("测试 JSON 反序列化")
        fun testDeserialization() {
            val jsonStr = """{"taskId": 100001, "nameEn": "test_task", "nameCn": "测试任务"}"""
            val task = json.decodeFromString<RemoteTaskInfo>(jsonStr)

            assertEquals(100001L, task.taskId)
            assertEquals("test_task", task.nameEn)
            assertEquals("测试任务", task.nameCn)
        }

        @Test
        @DisplayName("测试 JSON 序列化")
        fun testSerialization() {
            val task = RemoteTaskInfo(100001, "test_task", "测试任务")
            val jsonStr = json.encodeToString(RemoteTaskInfo.serializer(), task)

            assertTrue(jsonStr.contains("100001"))
            assertTrue(jsonStr.contains("test_task"))
            assertTrue(jsonStr.contains("测试任务"))
        }

        @Test
        @DisplayName("测试相等性")
        fun testEquality() {
            val task1 = RemoteTaskInfo(100001, "name", "名称")
            val task2 = RemoteTaskInfo(100001, "name", "名称")
            val task3 = RemoteTaskInfo(100002, "name", "名称")

            assertEquals(task1, task2)
            assertNotEquals(task1, task3)
        }
    }

    // ========== RemoteTaskListResponse 测试 ==========

    @Nested
    @DisplayName("RemoteTaskListResponse 测试")
    inner class RemoteTaskListResponseTests {

        @Test
        @DisplayName("测试正常响应")
        fun testNormalResponse() {
            val response = RemoteTaskListResponse(
                taskInfos = listOf(
                    RemoteTaskInfo(100001, "task1", "任务1"),
                    RemoteTaskInfo(100002, "task2", "任务2")
                )
            )

            assertEquals(2, response.getTaskList().size)
            assertEquals(100001L, response.getTaskList()[0].taskId)
        }

        @Test
        @DisplayName("测试 null taskInfos")
        fun testNullTaskInfos() {
            val response = RemoteTaskListResponse(taskInfos = null)
            assertTrue(response.getTaskList().isEmpty())
        }

        @Test
        @DisplayName("测试空列表")
        fun testEmptyTaskInfos() {
            val response = RemoteTaskListResponse(taskInfos = emptyList())
            assertTrue(response.getTaskList().isEmpty())
        }

        @Test
        @DisplayName("测试 JSON 反序列化")
        fun testDeserialization() {
            val jsonStr = """
                {
                    "taskInfos": [
                        {"taskId": 100001, "nameEn": "task1", "nameCn": "任务1"}
                    ]
                }
            """.trimIndent()

            val response = json.decodeFromString<RemoteTaskListResponse>(jsonStr)
            assertEquals(1, response.getTaskList().size)
            assertEquals("task1", response.getTaskList()[0].nameEn)
        }

        @Test
        @DisplayName("测试 JSON 反序列化 - 无 taskInfos 字段")
        fun testDeserializationMissingField() {
            val jsonStr = """{}"""
            val response = json.decodeFromString<RemoteTaskListResponse>(jsonStr)
            assertTrue(response.getTaskList().isEmpty())
        }
    }

    // ========== RemoteDefect 测试 ==========

    @Nested
    @DisplayName("RemoteDefect 测试")
    inner class RemoteDefectTests {

        @Test
        @DisplayName("测试基本创建")
        fun testCreation() {
            val defect = RemoteDefect(
                fileName = "src/main.go",
                filePath = "/project/src/main.go",
                lineNum = 42,
                author = listOf("user1"),
                checker = "errcheck",
                severity = 1,
                message = "Error return value is not checked",
                status = 1,
                toolName = "golangci-lint"
            )

            assertEquals("src/main.go", defect.fileName)
            assertEquals("/project/src/main.go", defect.filePath)
            assertEquals(42, defect.lineNum)
            assertEquals(listOf("user1"), defect.author)
            assertEquals("errcheck", defect.checker)
            assertEquals(1, defect.severity)
            assertEquals("Error return value is not checked", defect.message)
            assertEquals(1, defect.status)
            assertEquals("golangci-lint", defect.toolName)
        }

        @Test
        @DisplayName("测试 getSeverityText")
        fun testGetSeverityText() {
            assertEquals("严重", RemoteDefect(severity = 1).getSeverityText())
            assertEquals("一般", RemoteDefect(severity = 2).getSeverityText())
            assertEquals("提示", RemoteDefect(severity = 4).getSeverityText())
            assertEquals("建议", RemoteDefect(severity = 8).getSeverityText())
            assertEquals("未知", RemoteDefect(severity = 99).getSeverityText())
        }

        @Test
        @DisplayName("测试 getStatusText")
        fun testGetStatusText() {
            assertEquals("待修复", RemoteDefect(status = 1).getStatusText())
            assertEquals("已修复", RemoteDefect(status = 2).getStatusText())
            assertEquals("已忽略", RemoteDefect(status = 4).getStatusText())
            assertEquals("未知", RemoteDefect(status = 99).getStatusText())
        }

        @Test
        @DisplayName("测试 getAuthorText")
        fun testGetAuthorText() {
            assertEquals("user1, user2", RemoteDefect(author = listOf("user1", "user2")).getAuthorText())
            assertEquals("user1", RemoteDefect(author = listOf("user1")).getAuthorText())
            assertEquals("", RemoteDefect(author = null).getAuthorText())
            assertEquals("", RemoteDefect(author = emptyList()).getAuthorText())
        }

        @Test
        @DisplayName("测试 toLocalDefect 转换")
        fun testToLocalDefect() {
            val remoteDefect = RemoteDefect(
                fileName = "src/main.go",
                filePath = "/project/src/main.go",
                lineNum = 42,
                author = listOf("user1"),
                checker = "errcheck",
                severity = 1,
                message = "Error not checked",
                status = 1,
                toolName = "golangci-lint"
            )

            val localDefect = remoteDefect.toLocalDefect()

            assertEquals("golangci-lint", localDefect.toolName)
            assertEquals("errcheck", localDefect.checkerName)
            assertEquals("Error not checked", localDefect.description)
            assertEquals("/project/src/main.go", localDefect.filePath)
            assertEquals(42, localDefect.line)
            assertEquals(1L, localDefect.severity)
        }

        @Test
        @DisplayName("测试默认值")
        fun testDefaultValues() {
            val defect = RemoteDefect()

            assertEquals("", defect.fileName)
            assertEquals("", defect.filePath)
            assertEquals(0, defect.lineNum)
            assertNull(defect.author)
            assertEquals("", defect.checker)
            assertEquals(4, defect.severity)
            assertEquals("", defect.message)
            assertEquals(1, defect.status)
            assertEquals("", defect.toolName)
        }

        @Test
        @DisplayName("测试 JSON 反序列化")
        fun testDeserialization() {
            val jsonStr = """
                {
                    "fileName": "main.go",
                    "filePath": "/project/main.go",
                    "lineNum": 10,
                    "author": ["user1"],
                    "checker": "errcheck",
                    "severity": 2,
                    "message": "test error",
                    "status": 1,
                    "toolName": "golangci-lint"
                }
            """.trimIndent()

            val defect = json.decodeFromString<RemoteDefect>(jsonStr)
            assertEquals("main.go", defect.fileName)
            assertEquals(10, defect.lineNum)
            assertEquals(2, defect.severity)
        }
    }

    // ========== RemoteDefectListResponse 测试 ==========

    @Nested
    @DisplayName("RemoteDefectListResponse 测试")
    inner class RemoteDefectListResponseTests {

        @Test
        @DisplayName("测试正常响应")
        fun testNormalResponse() {
            val response = RemoteDefectListResponse(
                seriousCount = 5,
                normalCount = 10,
                promptCount = 3,
                totalCount = 18,
                existCount = 15,
                fixCount = 2,
                ignoreCount = 1,
                defects = listOf(
                    RemoteDefect(fileName = "a.go", severity = 1),
                    RemoteDefect(fileName = "b.go", severity = 2)
                )
            )

            assertEquals(5, response.seriousCount)
            assertEquals(10, response.normalCount)
            assertEquals(3, response.promptCount)
            assertEquals(18, response.totalCount)
            assertEquals(15, response.existCount)
            assertEquals(2, response.fixCount)
            assertEquals(1, response.ignoreCount)
            assertEquals(2, response.getDefectList().size)
        }

        @Test
        @DisplayName("测试 null defects")
        fun testNullDefects() {
            val response = RemoteDefectListResponse(defects = null)
            assertTrue(response.getDefectList().isEmpty())
        }

        @Test
        @DisplayName("测试 getSummaryText")
        fun testGetSummaryText() {
            val response = RemoteDefectListResponse(
                seriousCount = 5,
                normalCount = 10,
                promptCount = 3,
                totalCount = 18
            )

            val summary = response.getSummaryText()
            assertTrue(summary.contains("18"))
            assertTrue(summary.contains("5"))
            assertTrue(summary.contains("10"))
            assertTrue(summary.contains("3"))
        }

        @Test
        @DisplayName("测试默认值")
        fun testDefaultValues() {
            val response = RemoteDefectListResponse()

            assertEquals(0, response.seriousCount)
            assertEquals(0, response.normalCount)
            assertEquals(0, response.promptCount)
            assertEquals(0, response.totalCount)
            assertEquals(0, response.existCount)
            assertEquals(0, response.fixCount)
            assertEquals(0, response.ignoreCount)
            assertTrue(response.getDefectList().isEmpty())
        }

        @Test
        @DisplayName("测试 JSON 反序列化")
        fun testDeserialization() {
            val jsonStr = """
                {
                    "seriousCount": 5,
                    "normalCount": 10,
                    "promptCount": 3,
                    "totalCount": 18,
                    "existCount": 15,
                    "fixCount": 2,
                    "ignoreCount": 1,
                    "defects": [
                        {
                            "fileName": "main.go",
                            "filePath": "/project/main.go",
                            "lineNum": 42,
                            "author": ["user1"],
                            "checker": "errcheck",
                            "severity": 1,
                            "message": "Error not checked",
                            "status": 1,
                            "toolName": "golangci-lint"
                        }
                    ]
                }
            """.trimIndent()

            val response = json.decodeFromString<RemoteDefectListResponse>(jsonStr)
            assertEquals(18, response.totalCount)
            assertEquals(1, response.getDefectList().size)
            assertEquals("main.go", response.getDefectList()[0].fileName)
        }

        @Test
        @DisplayName("测试 JSON 反序列化 - 空响应")
        fun testDeserializationEmpty() {
            val jsonStr = """{}"""
            val response = json.decodeFromString<RemoteDefectListResponse>(jsonStr)
            assertEquals(0, response.totalCount)
            assertTrue(response.getDefectList().isEmpty())
        }
    }

    // ========== RemoteDefectListRequest 测试 ==========

    @Nested
    @DisplayName("RemoteDefectListRequest 测试")
    inner class RemoteDefectListRequestTests {

        @Test
        @DisplayName("测试最小请求 - 只有必填字段")
        fun testMinimalRequest() {
            val request = RemoteDefectListRequest(
                projectRoot = "/project/root"
            )

            assertEquals("/project/root", request.projectRoot)
            assertNull(request.taskIdList)
            assertNull(request.dimensionList)
            assertNull(request.checker)
            assertNull(request.pageNum)
            assertNull(request.pageSize)
        }

        @Test
        @DisplayName("测试完整请求")
        fun testFullRequest() {
            val request = RemoteDefectListRequest(
                projectRoot = "/project",
                taskIdList = listOf(100001L),
                toolNameList = listOf("golangci-lint"),
                dimensionList = listOf("DEFECT"),
                checker = "errcheck",
                author = "user1",
                severity = listOf("1", "2"),
                status = listOf("1"),
                pageNum = 1,
                pageSize = 50,
                sortField = "fileName",
                sortType = "ASC"
            )

            assertEquals("/project", request.projectRoot)
            assertEquals(listOf(100001L), request.taskIdList)
            assertEquals(listOf("golangci-lint"), request.toolNameList)
            assertEquals(listOf("DEFECT"), request.dimensionList)
            assertEquals("errcheck", request.checker)
            assertEquals("user1", request.author)
            assertEquals(listOf("1", "2"), request.severity)
            assertEquals(listOf("1"), request.status)
            assertEquals(1, request.pageNum)
            assertEquals(50, request.pageSize)
            assertEquals("fileName", request.sortField)
            assertEquals("ASC", request.sortType)
        }

        @Test
        @DisplayName("测试 JSON 序列化")
        fun testSerialization() {
            val request = RemoteDefectListRequest(
                projectRoot = "/project",
                taskIdList = listOf(100001L),
                dimensionList = listOf("DEFECT"),
                pageNum = 1,
                pageSize = 100
            )

            val jsonStr = json.encodeToString(RemoteDefectListRequest.serializer(), request)
            assertTrue(jsonStr.contains("/project"))
            assertTrue(jsonStr.contains("100001"))
            assertTrue(jsonStr.contains("DEFECT"))
        }

        @Test
        @DisplayName("测试相等性")
        fun testEquality() {
            val req1 = RemoteDefectListRequest("/project", taskIdList = listOf(1L))
            val req2 = RemoteDefectListRequest("/project", taskIdList = listOf(1L))
            val req3 = RemoteDefectListRequest("/other", taskIdList = listOf(1L))

            assertEquals(req1, req2)
            assertNotEquals(req1, req3)
        }
    }
}
