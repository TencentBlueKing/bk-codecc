package com.codecc.preci.api.model.response

import com.codecc.preci.BaseTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * GetProjectResponse 数据模型测试
 *
 * 测试获取当前绑定项目响应的序列化/反序列化功能。
 *
 * @since 1.0
 */
@DisplayName("GetProjectResponse 测试")
class GetProjectResponseTest : BaseTest() {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    @DisplayName("测试 GetProjectResponse 数据类创建")
    fun testGetProjectResponseCreation() {
        val response = GetProjectResponse(projectId = "project123")

        assertEquals("project123", response.projectId)
    }

    @Test
    @DisplayName("测试 GetProjectResponse 空项目 ID")
    fun testGetProjectResponseEmptyProjectId() {
        val response = GetProjectResponse(projectId = "")

        assertEquals("", response.projectId)
    }

    @Test
    @DisplayName("测试 GetProjectResponse 相等性")
    fun testGetProjectResponseEquality() {
        val response1 = GetProjectResponse(projectId = "project123")
        val response2 = GetProjectResponse(projectId = "project123")
        val response3 = GetProjectResponse(projectId = "project456")

        assertEquals(response1, response2)
        assertNotEquals(response1, response3)
    }

    @Test
    @DisplayName("测试 GetProjectResponse JSON 反序列化")
    fun testGetProjectResponseDeserialization() {
        val jsonString = """{"projectId":"project123"}"""
        val response = json.decodeFromString<GetProjectResponse>(jsonString)

        assertEquals("project123", response.projectId)
    }

    @Test
    @DisplayName("测试 GetProjectResponse JSON 反序列化 - 空项目 ID")
    fun testGetProjectResponseDeserializationEmpty() {
        val jsonString = """{"projectId":""}"""
        val response = json.decodeFromString<GetProjectResponse>(jsonString)

        assertEquals("", response.projectId)
    }

    @Test
    @DisplayName("测试 GetProjectResponse JSON 序列化")
    fun testGetProjectResponseSerialization() {
        val response = GetProjectResponse(projectId = "project123")
        val jsonString = json.encodeToString(response)

        assertTrue(jsonString.contains("\"projectId\""))
        assertTrue(jsonString.contains("\"project123\""))
    }

    @Test
    @DisplayName("测试 GetProjectResponse 序列化往返")
    fun testGetProjectResponseRoundTrip() {
        val original = GetProjectResponse(projectId = "test_project_001")
        val jsonString = json.encodeToString(original)
        val deserialized = json.decodeFromString<GetProjectResponse>(jsonString)

        assertEquals(original, deserialized)
    }

    @Test
    @DisplayName("测试 GetProjectResponse toString")
    fun testGetProjectResponseToString() {
        val response = GetProjectResponse(projectId = "project123")
        val toString = response.toString()

        assertTrue(toString.contains("GetProjectResponse"))
        assertTrue(toString.contains("project123"))
    }

    @Test
    @DisplayName("测试 GetProjectResponse hashCode")
    fun testGetProjectResponseHashCode() {
        val response1 = GetProjectResponse(projectId = "project123")
        val response2 = GetProjectResponse(projectId = "project123")

        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    @DisplayName("测试 GetProjectResponse copy")
    fun testGetProjectResponseCopy() {
        val original = GetProjectResponse(projectId = "project123")
        val copy = original.copy(projectId = "project456")

        assertEquals("project123", original.projectId)
        assertEquals("project456", copy.projectId)
        assertNotEquals(original, copy)
    }
}

