package com.codecc.preci.api.model

import com.codecc.preci.api.model.request.*
import com.codecc.preci.api.model.response.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 数据模型序列化/反序列化测试
 *
 * 测试所有请求和响应数据类的 JSON 序列化和反序列化功能
 *
 * @since 1.0
 */
@DisplayName("数据模型序列化测试")
class SerializationTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    @DisplayName("LoginRequest 序列化")
    fun `should serialize LoginRequest correctly`() {
        val request = LoginRequest(pinToken = "12345:abcdef", projectId = "test_project")
        val jsonString = json.encodeToString(request)
        
        assert(jsonString.contains("\"pinToken\":\"12345:abcdef\""))
        assert(jsonString.contains("\"projectId\":\"test_project\""))
    }

    @Test
    @DisplayName("LoginRequest 反序列化")
    fun `should deserialize LoginRequest correctly`() {
        val jsonString = """{"pinToken":"12345:abcdef","projectId":"test_project"}"""
        val request = json.decodeFromString<LoginRequest>(jsonString)
        
        assertEquals("12345:abcdef", request.pinToken)
        assertEquals("test_project", request.projectId)
    }

    @Test
    @DisplayName("InitRequest 序列化")
    fun `should serialize InitRequest correctly`() {
        val request = InitRequest(currentPath = "/path/to/project", rootPath = "/path/to/root")
        val jsonString = json.encodeToString(request)
        
        assert(jsonString.contains("\"currentPath\":\"/path/to/project\""))
        assert(jsonString.contains("\"rootPath\":\"/path/to/root\""))
    }

    @Test
    @DisplayName("ScanRequest 序列化")
    fun `should serialize ScanRequest correctly`() {
        val request = ScanRequest(scanType = 1, paths = listOf("/path1", "/path2"), rootDir = "/root")
        val jsonString = json.encodeToString(request)
        
        assert(jsonString.contains("\"scanType\":1"))
        assert(jsonString.contains("\"/path1\""))
        assert(jsonString.contains("\"/path2\""))
    }

    @Test
    @DisplayName("LoginResponse 反序列化")
    fun `should deserialize LoginResponse correctly`() {
        val jsonString = """{"projectId":"proj123","userId":"user456"}"""
        val response = json.decodeFromString<LoginResponse>(jsonString)
        
        assertEquals("proj123", response.projectId)
        assertEquals("user456", response.userId)
    }

    @Test
    @DisplayName("InitResponse 反序列化")
    fun `should deserialize InitResponse correctly`() {
        val jsonString = """{"rootPath":"/project/root"}"""
        val response = json.decodeFromString<InitResponse>(jsonString)
        
        assertEquals("/project/root", response.rootPath)
    }

    @Test
    @DisplayName("ScanResponse 反序列化")
    fun `should deserialize ScanResponse correctly`() {
        val jsonString = """{"message":"Scan started","tools":["tool1","tool2"],"scanFileNum":100}"""
        val response = json.decodeFromString<ScanResponse>(jsonString)
        
        assertEquals("Scan started", response.message)
        assertEquals(2, response.tools.size)
        assertEquals("tool1", response.tools[0])
        assertEquals(100, response.scanFileNum)
    }

    @Test
    @DisplayName("ScanProgressResponse 反序列化")
    fun `should deserialize ScanProgressResponse correctly`() {
        val jsonString = """{"projectRoot":"/root","toolStatuses":{"tool1":"running","tool2":"done"},"status":"running"}"""
        val response = json.decodeFromString<ScanProgressResponse>(jsonString)
        
        assertEquals("/root", response.projectRoot)
        assertEquals("running", response.toolStatuses["tool1"])
        assertEquals("done", response.toolStatuses["tool2"])
        assertEquals("running", response.status)
    }

    @Test
    @DisplayName("ScanResultResponse 反序列化（不带 severity）")
    fun `should deserialize ScanResultResponse correctly`() {
        val jsonString = """
            {
                "defects": [
                    {
                        "toolName": "golangci-lint",
                        "checkerName": "errcheck",
                        "description": "Error not checked",
                        "filePath": "/path/to/file.go",
                        "line": 42
                    }
                ]
            }
        """.trimIndent()
        
        val response = json.decodeFromString<ScanResultResponse>(jsonString)
        
        val defects = response.getDefectList()
        assertEquals(1, defects.size)
        val defect = defects[0]
        assertEquals("golangci-lint", defect.toolName)
        assertEquals("errcheck", defect.checkerName)
        assertEquals("Error not checked", defect.description)
        assertEquals("/path/to/file.go", defect.filePath)
        assertEquals(42, defect.line)
        assertEquals(4L, defect.severity) // 默认值为 4（提示）
        assertEquals("提示", defect.getSeverityText())
    }

    @Test
    @DisplayName("ScanResultResponse 反序列化（带 severity）")
    fun `should deserialize ScanResultResponse with severity correctly`() {
        val jsonString = """
            {
                "defects": [
                    {
                        "toolName": "golangci-lint",
                        "checkerName": "errcheck",
                        "description": "Error not checked",
                        "filePath": "/path/to/file.go",
                        "line": 42,
                        "severity": 1
                    },
                    {
                        "toolName": "golangci-lint",
                        "checkerName": "ineffassign",
                        "description": "Ineffectual assignment",
                        "filePath": "/path/to/file2.go",
                        "line": 15,
                        "severity": 2
                    },
                    {
                        "toolName": "golangci-lint",
                        "checkerName": "deadcode",
                        "description": "Unused code",
                        "filePath": "/path/to/file3.go",
                        "line": 20,
                        "severity": 4
                    }
                ]
            }
        """.trimIndent()
        
        val response = json.decodeFromString<ScanResultResponse>(jsonString)
        
        val defects = response.getDefectList()
        assertEquals(3, defects.size)
        
        // 测试严重级别 1 - 严重
        val defect1 = defects[0]
        assertEquals(1L, defect1.severity)
        assertEquals("严重", defect1.getSeverityText())
        
        // 测试严重级别 2 - 一般
        val defect2 = defects[1]
        assertEquals(2L, defect2.severity)
        assertEquals("一般", defect2.getSeverityText())
        
        // 测试严重级别 4 - 提示
        val defect3 = defects[2]
        assertEquals(4L, defect3.severity)
        assertEquals("提示", defect3.getSeverityText())
    }

    @Test
    @DisplayName("Defect 未知 severity 处理")
    fun `should handle unknown severity values`() {
        val jsonString = """
            {
                "defects": [
                    {
                        "toolName": "tool",
                        "checkerName": "checker",
                        "description": "desc",
                        "filePath": "/path",
                        "line": 1,
                        "severity": 99
                    }
                ]
            }
        """.trimIndent()
        
        val response = json.decodeFromString<ScanResultResponse>(jsonString)
        val defect = response.getDefectList()[0]
        
        assertEquals(99L, defect.severity)
        assertEquals("未知(99)", defect.getSeverityText())
    }

    @Test
    @DisplayName("CheckerSetListResponse 反序列化")
    fun `should deserialize CheckerSetListResponse correctly`() {
        val jsonString = """
            {
                "checkerSets": [
                    {
                        "checkerSetId": "set1",
                        "checkerSetName": "Standard Set",
                        "toolName": "tool1"
                    }
                ]
            }
        """.trimIndent()
        
        val response = json.decodeFromString<CheckerSetListResponse>(jsonString)
        
        assertEquals(1, response.checkerSets.size)
        val checkerSet = response.checkerSets[0]
        assertEquals("set1", checkerSet.checkerSetId)
        assertEquals("Standard Set", checkerSet.checkerSetName)
        assertEquals("tool1", checkerSet.toolName)
    }

    @Test
    @DisplayName("ErrorResponse 反序列化")
    fun `should deserialize ErrorResponse correctly`() {
        val jsonString = """{"error":"Something went wrong"}"""
        val response = json.decodeFromString<ErrorResponse>(jsonString)
        
        assertEquals("Something went wrong", response.error)
    }

    @Test
    @DisplayName("LatestVersionResponse 反序列化")
    fun `should deserialize LatestVersionResponse correctly`() {
        val jsonString = """{"latestVersion":"v2.1.0"}"""
        val response = json.decodeFromString<LatestVersionResponse>(jsonString)
        
        assertEquals("v2.1.0", response.latestVersion)
    }

    @Test
    @DisplayName("处理缺失可选字段")
    fun `should handle missing optional fields`() {
        // LoginRequest 的 projectId 是可选的
        val jsonString = """{"pinToken":"12345:abcdef"}"""
        val request = json.decodeFromString<LoginRequest>(jsonString)
        
        assertEquals("12345:abcdef", request.pinToken)
        assertEquals(null, request.projectId)
    }

    @Test
    @DisplayName("处理空数组")
    fun `should handle empty arrays`() {
        val jsonString = """{"defects":[]}"""
        val response = json.decodeFromString<ScanResultResponse>(jsonString)
        
        assertEquals(0, response.getDefectList().size)
    }

    @Test
    @DisplayName("处理 null defects")
    fun `should handle null defects`() {
        val jsonString = """{"defects":null}"""
        val response = json.decodeFromString<ScanResultResponse>(jsonString)
        
        // getDefectList() 应该返回空列表而不是 null
        assertEquals(0, response.getDefectList().size)
    }
}

