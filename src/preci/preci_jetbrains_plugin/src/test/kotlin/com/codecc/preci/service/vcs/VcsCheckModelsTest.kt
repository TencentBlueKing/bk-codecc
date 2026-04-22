package com.codecc.preci.service.vcs

import com.codecc.preci.api.model.response.Defect
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * VcsCheckModels 单元测试
 *
 * 验证 VCS 检查数据模型的基本行为和属性。
 *
 * @since 1.1
 */
@DisplayName("VcsCheckModels 测试")
class VcsCheckModelsTest {

    @Nested
    @DisplayName("GitHookScanScope 测试")
    inner class GitHookScanScopeTests {

        @Test
        @DisplayName("CHANGED_FILES_COMMIT 的 scanType 应为 102")
        fun `CHANGED_FILES_COMMIT should have scanType 102`() {
            assertEquals(102, GitHookScanScope.CHANGED_FILES_COMMIT.scanType)
        }

        @Test
        @DisplayName("CHANGED_FILES_PUSH 的 scanType 应为 103")
        fun `CHANGED_FILES_PUSH should have scanType 103`() {
            assertEquals(103, GitHookScanScope.CHANGED_FILES_PUSH.scanType)
        }

        @Test
        @DisplayName("ALL_FILES 的 scanType 应为 0")
        fun `ALL_FILES should have scanType 0`() {
            assertEquals(0, GitHookScanScope.ALL_FILES.scanType)
        }
    }

    @Nested
    @DisplayName("VcsCheckResult 测试")
    inner class VcsCheckResultTests {

        @Test
        @DisplayName("Disabled 是单例")
        fun `Disabled should be a singleton`() {
            assertSame(VcsCheckResult.Disabled, VcsCheckResult.Disabled)
        }

        @Test
        @DisplayName("NoDefects 包含正确的消息")
        fun `NoDefects should contain correct message`() {
            val result = VcsCheckResult.NoDefects("检查通过")
            assertEquals("检查通过", result.message)
        }

        @Test
        @DisplayName("HasDefects 包含正确的缺陷数和列表")
        fun `HasDefects should contain correct defect count and list`() {
            val defects = listOf(
                Defect("lint", "errcheck", "error not checked", "/test.go", 42),
                Defect("lint", "unused", "unused variable", "/test.go", 10)
            )
            val result = VcsCheckResult.HasDefects(defectCount = 2, defects = defects)
            assertEquals(2, result.defectCount)
            assertEquals(2, result.defects.size)
            assertEquals("errcheck", result.defects[0].checkerName)
        }

        @Test
        @DisplayName("Error 包含消息和可选异常")
        fun `Error should contain message and optional exception`() {
            val exception = RuntimeException("test error")
            val withException = VcsCheckResult.Error("扫描失败", exception)
            val withoutException = VcsCheckResult.Error("未知错误")

            assertEquals("扫描失败", withException.message)
            assertSame(exception, withException.exception)
            assertEquals("未知错误", withoutException.message)
            assertNull(withoutException.exception)
        }

        @Test
        @DisplayName("sealed class 子类可通过 when 完整匹配")
        fun `all subclasses should be matchable via when expression`() {
            val results: List<VcsCheckResult> = listOf(
                VcsCheckResult.Disabled,
                VcsCheckResult.NoDefects("ok"),
                VcsCheckResult.HasDefects(1, listOf(Defect("t", "c", "d", "/f", 1))),
                VcsCheckResult.Error("err")
            )

            results.forEach { result ->
                val matched = when (result) {
                    is VcsCheckResult.Disabled -> "disabled"
                    is VcsCheckResult.NoDefects -> "no_defects"
                    is VcsCheckResult.HasDefects -> "has_defects"
                    is VcsCheckResult.Error -> "error"
                }
                assertNotNull(matched)
            }
        }
    }
}
