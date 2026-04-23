package com.codecc.preci.api.model.response

import com.codecc.preci.BaseTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 流水线 API 响应模型 JSON 序列化/反序列化测试
 *
 * @since 1.0
 */
@DisplayName("PipelineBuildResponse 模型序列化测试")
class PipelineBuildResponseTest : BaseTest() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    @DisplayName("PipelineBuild 全字段序列化与反序列化")
    fun `PipelineBuild should round-trip with all fields`() {
        val original = PipelineBuild(
            buildId = "b-001",
            buildNum = 42L,
            startTime = 1700000000000L,
            endTime = 1700000600000L,
            status = "SUCCEED"
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<PipelineBuild>(encoded)

        assertEquals(original, decoded)
        assertTrue(encoded.contains("\"buildId\":\"b-001\""))
        assertTrue(encoded.contains("\"buildNum\":42"))
    }

    @Test
    @DisplayName("PipelineBuildHistoryResponse 空列表")
    fun `PipelineBuildHistoryResponse should handle empty builds`() {
        val empty = PipelineBuildHistoryResponse(builds = emptyList())
        val encoded = json.encodeToString(empty)
        val decoded = json.decodeFromString<PipelineBuildHistoryResponse>(encoded)

        assertEquals(0, decoded.builds?.size)
    }

    @Test
    @DisplayName("PipelineBuildHistoryResponse 多条构建记录")
    fun `PipelineBuildHistoryResponse should deserialize normal list`() {
        val jsonStr = """
            {
                "builds": [
                    {"buildId":"a","buildNum":1,"startTime":1,"endTime":2,"status":"RUNNING"},
                    {"buildId":"b","buildNum":2,"startTime":3,"endTime":0,"status":"QUEUE"}
                ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<PipelineBuildHistoryResponse>(jsonStr)

        assertEquals(2, parsed.builds?.size)
        assertEquals("a", parsed.builds?.get(0)?.buildId)
        assertEquals("QUEUE", parsed.builds?.get(1)?.status)

        val roundTrip = json.encodeToString(parsed)
        val again = json.decodeFromString<PipelineBuildHistoryResponse>(roundTrip)
        assertEquals(parsed, again)
    }

    @Test
    @DisplayName("StartBuildResponse 序列化与反序列化")
    fun `StartBuildResponse should round-trip`() {
        val original = StartBuildResponse(buildId = "new-build-99")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<StartBuildResponse>(encoded)

        assertEquals(original, decoded)
        assertTrue(encoded.contains("\"buildId\":\"new-build-99\""))
    }

    @Test
    @DisplayName("PipelineBuildLog 序列化与反序列化")
    fun `PipelineBuildLog should round-trip`() {
        val original = PipelineBuildLog(
            lineNo = 100L,
            message = "compile ok",
            timestamp = 1700000000123L
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<PipelineBuildLog>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    @DisplayName("PipelineBuildLogsResponse 含日志且未完成")
    fun `PipelineBuildLogsResponse should round-trip with logs and finished false`() {
        val original = PipelineBuildLogsResponse(
            buildId = "bid-1",
            finished = false,
            logs = listOf(
                PipelineBuildLog(1L, "line1", 10L),
                PipelineBuildLog(2L, "line2", 11L)
            )
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<PipelineBuildLogsResponse>(encoded)

        assertEquals(original, decoded)
        assertEquals(2, decoded.logs?.size)
        assertFalse(decoded.finished)
    }

    @Test
    @DisplayName("PipelineBuildLogsResponse 空日志且已完成")
    fun `PipelineBuildLogsResponse should handle empty logs and finished true`() {
        val original = PipelineBuildLogsResponse(
            buildId = "bid-2",
            finished = true,
            logs = emptyList()
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<PipelineBuildLogsResponse>(encoded)

        assertEquals(original, decoded)
        assertTrue(decoded.logs?.isEmpty() ?: true)
        assertTrue(decoded.finished)
    }

    @Test
    @DisplayName("PipelineBuildDetailResponse 全字段")
    fun `PipelineBuildDetailResponse should round-trip with all fields`() {
        val original = PipelineBuildDetailResponse(
            id = "detail-id",
            status = "FAILED",
            startTime = 100L,
            endTime = 200L,
            buildNum = 7L
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<PipelineBuildDetailResponse>(encoded)

        assertEquals(original, decoded)
        assertTrue(encoded.contains("\"id\":\"detail-id\""))
        assertTrue(encoded.contains("\"buildNum\":7"))
    }
}
