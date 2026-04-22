package com.codecc.preci.util

import com.codecc.preci.BaseTest
import com.codecc.preci.core.http.ServerNotRunningException
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/**
 * PreCIPortDetector 集成测试
 *
 * 此测试会实际执行 `preci port` 命令来验证 PreCIPortDetector 的功能。
 *
 * **注意事项：**
 * - 如果环境中没有安装 PreCI CLI，测试会抛出 ServerNotRunningException
 * - 如果 Local Server 未启动，测试同样会抛出 ServerNotRunningException
 * - 只有在 PreCI CLI 已安装且 Local Server 正在运行时，测试才会成功获取端口号
 *
 * @since 1.0
 */
@DisplayName("PreCIPortDetector 集成测试")
class PreCIPortDetectorTest : BaseTest() {

    @Test
    @DisplayName("获取服务端口 - 实际调用 preci port 命令")
    fun `getServerPort should execute preci port command`() {
        try {
            // 实际调用 preci port 命令
            val port = PreCIPortDetector.getServerPort()

            // 如果成功获取端口，验证端口号在有效范围内
            assertTrue(port in 1024..65535) {
                "端口号应该在有效范围内 (1024-65535)，实际值: $port"
            }

            println("成功获取 PreCI Local Server 端口: $port")

        } catch (e: ServerNotRunningException) {
            // 如果 PreCI CLI 未安装或 Server 未运行，这是预期的行为
            println("预期的异常 - PreCI 环境不可用: ${e.message}")

            // 验证异常消息包含有用的信息
            assertTrue(e.message != null && e.message!!.isNotBlank()) {
                "异常消息不应为空"
            }
        }
    }

    @Test
    @DisplayName("getServerPort 应该抛出 ServerNotRunningException 或返回有效端口")
    fun `getServerPort should either return valid port or throw ServerNotRunningException`() {
        // 此测试验证方法的行为是确定性的：
        // 要么返回有效端口，要么抛出 ServerNotRunningException

        val result = runCatching {
            PreCIPortDetector.getServerPort()
        }

        result.onSuccess { port ->
            // 成功时，端口号应该有效
            assertTrue(port in 1024..65535) {
                "返回的端口号应该在有效范围内"
            }
        }

        result.onFailure { exception ->
            // 失败时，应该是 ServerNotRunningException
            assertTrue(exception is ServerNotRunningException) {
                "应该抛出 ServerNotRunningException，而不是 ${exception::class.simpleName}"
            }
        }
    }

    @Test
    @DisplayName("多次调用 getServerPort 应该返回一致的结果")
    fun `getServerPort should return consistent results on multiple calls`() {
        try {
            // 第一次调用
            val port1 = PreCIPortDetector.getServerPort()

            // 第二次调用
            val port2 = PreCIPortDetector.getServerPort()

            // 两次调用应该返回相同的端口号（假设 Server 状态未变）
            assertTrue(port1 == port2) {
                "多次调用应该返回相同的端口号，但得到 $port1 和 $port2"
            }

            println("端口号一致性验证通过: $port1")

        } catch (e: ServerNotRunningException) {
            // PreCI 环境不可用，跳过一致性测试
            println("跳过一致性测试 - PreCI 环境不可用: ${e.message}")
        }
    }

    @Test
    @DisplayName("异常消息应该包含有用的诊断信息")
    fun `exception message should contain helpful diagnostic info`() {
        try {
            PreCIPortDetector.getServerPort()
            // 如果成功，说明环境正常，测试通过
            println("PreCI 环境正常，无需验证异常消息")

        } catch (e: ServerNotRunningException) {
            val message = e.message ?: ""

            // 验证异常消息包含有用的信息
            val containsHelpfulInfo = message.contains("preci", ignoreCase = true) ||
                    message.contains("Server", ignoreCase = true) ||
                    message.contains("CLI", ignoreCase = true) ||
                    message.contains("port", ignoreCase = true) ||
                    message.contains("running", ignoreCase = true)

            assertTrue(containsHelpfulInfo) {
                "异常消息应该包含有用的诊断信息，实际消息: $message"
            }

            println("异常消息验证通过: $message")
        }
    }
}

