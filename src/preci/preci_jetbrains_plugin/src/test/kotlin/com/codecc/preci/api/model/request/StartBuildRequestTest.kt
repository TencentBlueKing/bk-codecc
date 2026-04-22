package com.codecc.preci.api.model.request

import com.codecc.preci.BaseTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * StartBuildRequest 序列化测试
 *
 * @since 1.0
 */
@DisplayName("StartBuildRequest 序列化测试")
class StartBuildRequestTest : BaseTest() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    @DisplayName("序列化为 JSON 并校验 rootPath 字段")
    fun `should serialize to JSON with rootPath`() {
        val request = StartBuildRequest(rootPath = "/Users/demo/my-project")
        val jsonString = json.encodeToString(request)

        assertTrue(jsonString.contains("\"rootPath\":\"/Users/demo/my-project\""))
        assertTrue(jsonString.contains("rootPath"))
    }

    @Test
    @DisplayName("从 JSON 字符串反序列化")
    fun `should deserialize from JSON string`() {
        val jsonString = """{"rootPath":"/var/workspace/app"}"""
        val request = json.decodeFromString<StartBuildRequest>(jsonString)

        assertEquals("/var/workspace/app", request.rootPath)
    }

    @Test
    @DisplayName("序列化后再反序列化保持一致")
    fun `encode then decode should preserve rootPath`() {
        val original = StartBuildRequest(rootPath = "D:/repo/preci")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<StartBuildRequest>(encoded)

        assertEquals(original, decoded)
    }
}
