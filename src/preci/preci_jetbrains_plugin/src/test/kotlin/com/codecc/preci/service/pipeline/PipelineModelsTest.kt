package com.codecc.preci.service.pipeline

import com.codecc.preci.BaseTest
import com.codecc.preci.api.model.response.PipelineBuild
import com.codecc.preci.api.model.response.PipelineBuildDetailResponse
import com.codecc.preci.api.model.response.PipelineBuildLog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 流水线服务结果密封类单元测试
 *
 * @since 1.0
 */
@DisplayName("Pipeline 结果密封类测试")
class PipelineModelsTest : BaseTest() {

    @Test
    @DisplayName("BuildHistoryResult Success 与 Failure")
    fun `BuildHistoryResult Success and Failure`() {
        val builds = listOf(PipelineBuild("b1", 1, 0, 0, "OK"))
        val success: BuildHistoryResult = BuildHistoryResult.Success(builds)
        assertTrue(success is BuildHistoryResult.Success)
        assertEquals(1, (success as BuildHistoryResult.Success).builds.size)

        val fail: BuildHistoryResult = BuildHistoryResult.Failure("失败", RuntimeException("x"))
        assertTrue(fail is BuildHistoryResult.Failure)
        assertEquals("失败", (fail as BuildHistoryResult.Failure).message)
        assertNotNull(fail.cause)
    }

    @Test
    @DisplayName("StartBuildResult Success 与 Failure")
    fun `StartBuildResult Success and Failure`() {
        val success: StartBuildResult = StartBuildResult.Success("bid-1")
        assertTrue(success is StartBuildResult.Success)
        assertEquals("bid-1", (success as StartBuildResult.Success).buildId)

        val fail: StartBuildResult = StartBuildResult.Failure("无法触发")
        assertTrue(fail is StartBuildResult.Failure)
        assertEquals("无法触发", (fail as StartBuildResult.Failure).message)
        assertNull(fail.cause)
    }

    @Test
    @DisplayName("BuildLogsResult Success 与 Failure")
    fun `BuildLogsResult Success and Failure`() {
        val logs = listOf(PipelineBuildLog(1L, "m", 2L))
        val success: BuildLogsResult = BuildLogsResult.Success(logs, finished = true)
        assertTrue(success is BuildLogsResult.Success)
        assertTrue((success as BuildLogsResult.Success).finished)
        assertEquals(1, success.logs.size)

        val fail: BuildLogsResult = BuildLogsResult.Failure("读日志失败", IllegalStateException())
        assertTrue(fail is BuildLogsResult.Failure)
        assertEquals("读日志失败", (fail as BuildLogsResult.Failure).message)
    }

    @Test
    @DisplayName("BuildDetailResult Success 与 Failure")
    fun `BuildDetailResult Success and Failure`() {
        val detail = PipelineBuildDetailResponse("id1", "SUCCEED", 1L, 2L, 3L)
        val success: BuildDetailResult = BuildDetailResult.Success(detail)
        assertTrue(success is BuildDetailResult.Success)
        assertEquals("id1", (success as BuildDetailResult.Success).detail.id)

        val fail: BuildDetailResult = BuildDetailResult.Failure("无详情")
        assertTrue(fail is BuildDetailResult.Failure)
    }

    @Test
    @DisplayName("StopBuildResult Success 与 Failure")
    fun `StopBuildResult Success and Failure`() {
        val success: StopBuildResult = StopBuildResult.Success
        assertTrue(success is StopBuildResult.Success)

        val fail: StopBuildResult = StopBuildResult.Failure("停不了", Exception("e"))
        assertTrue(fail is StopBuildResult.Failure)
        assertEquals("停不了", (fail as StopBuildResult.Failure).message)
    }

    @Test
    @DisplayName("流水线结果 when 分支类型安全")
    fun `sealed results should work in when expressions`() {
        val history: BuildHistoryResult = BuildHistoryResult.Success(emptyList())
        val h = when (history) {
            is BuildHistoryResult.Success -> "ok:${history.builds.size}"
            is BuildHistoryResult.Failure -> history.message
        }
        assertTrue(h.startsWith("ok:"))

        val stop: StopBuildResult = StopBuildResult.Failure("m")
        val s = when (stop) {
            StopBuildResult.Success -> "s"
            is StopBuildResult.Failure -> stop.message
        }
        assertEquals("m", s)
    }
}
