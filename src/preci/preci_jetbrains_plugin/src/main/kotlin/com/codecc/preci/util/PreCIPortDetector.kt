package com.codecc.preci.util

import com.codecc.preci.core.http.ServerNotRunningException
import com.codecc.preci.core.log.PreCILogger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * PreCI 端口检测工具
 *
 * 通过执行 `preci port` 命令读取 Local Server 的运行端口。
 * 该工具负责检测 PreCI Local Server 的运行状态和端口信息。
 *
 * **使用示例：**
 * ```kotlin
 * try {
 *     val port = PreCIPortDetector.getServerPort()
 *     println("Local Server running on port: $port")
 * } catch (e: ServerNotRunningException) {
 *     println("Local Server is not running")
 * }
 * ```
 *
 * @since 1.0
 */
object PreCIPortDetector {
    private val logger = PreCILogger.getLogger(PreCIPortDetector::class.java)

    /**
     * 获取 PreCI Local Server 运行端口
     *
     * 每次调用都会执行 `preci port` 命令实时获取当前端口，
     * 确保在 Server 重启或端口变更后始终返回正确的端口号。
     *
     * @return Local Server 的端口号
     * @throws ServerNotRunningException 如果 Server 未启动、命令执行失败或无法解析端口号
     *
     * @since 1.0
     */
    fun getServerPort(): Int {
        logger.debug("Detecting PreCI Local Server port...")

        try {
            val processBuilder = ShellCommandHelper.createProcessBuilder("preci", "port")
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readText()
            }

            val completed = process.waitFor(5, TimeUnit.SECONDS)

            if (!completed) {
                logger.warn("Command 'preci port' timed out")
                process.destroyForcibly()
                throw ServerNotRunningException(
                    "Command 'preci port' timed out. Local Server may not be running."
                )
            }

            val exitCode = process.exitValue()
            logger.debug("Command exit code: $exitCode, output: $output")

            if (exitCode != 0) {
                logger.warn("Command 'preci port' failed with exit code: $exitCode")
                throw ServerNotRunningException(
                    "PreCI Local Server is not running. Please start it using 'preci server start'."
                )
            }

            val port = parsePortFromOutput(output)
            logger.debug("Detected PreCI Local Server port: $port")

            return port

        } catch (e: ServerNotRunningException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to detect PreCI Local Server port", e)
            throw ServerNotRunningException(
                "Failed to detect PreCI Local Server. Ensure PreCI CLI is installed and in PATH. Error: ${e.message}",
            )
        }
    }

    /**
     * 从命令输出中解析端口号
     *
     * 从后往前逐行扫描，查找仅包含数字的行作为端口号。
     * 从后往前是因为 Shell 启动时的额外输出（oh-my-zsh、nvm 等）通常在前面，
     * `preci port` 的真正输出在最后。
     *
     * @param output 命令输出文本
     * @return 解析出的端口号
     * @throws ServerNotRunningException 如果无法解析出有效的端口号
     */
    private fun parsePortFromOutput(output: String): Int {
        val lines = output.trim().lines()

        for (line in lines.asReversed()) {
            val trimmed = line.trim()
            if (trimmed.matches(Regex("""^\d+$"""))) {
                val port = trimmed.toIntOrNull()
                if (port != null && port in 1024..65535) {
                    return port
                }
            }
        }

        logger.error("Failed to parse port from command output. Output: $output")
        throw ServerNotRunningException("Get Local server port fail. Local Server may not be running.")
    }
}

