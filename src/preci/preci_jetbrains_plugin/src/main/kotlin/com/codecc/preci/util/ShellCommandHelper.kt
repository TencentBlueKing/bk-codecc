package com.codecc.preci.util

import com.codecc.preci.core.config.PreCISettings
import com.codecc.preci.core.log.PreCILogger
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * PreCI CLI 路径解析与命令执行辅助工具
 *
 * 解决 macOS/Linux 上 GUI 应用（从 Dock/Spotlight 启动 IDE）PATH 不完整的问题。
 * 对 `preci` 命令提供专门的解析策略：
 *   1. 优先使用用户在 Settings 中配置的路径
 *   2. 优先搜索 ~/PreCI 目录（含一级子目录）
 *   3. 搜索常见安装目录和系统 PATH
 *
 * 还提供 [detectPreCI] 方法，用于检测安装状态与版本。
 */
object ShellCommandHelper {
    private val logger = PreCILogger.getLogger(ShellCommandHelper::class.java)

    private val searchDirs: List<String> by lazy { buildSearchDirs() }

    // ======================== 检测结果类型 ========================

    enum class DetectionStatus {
        /** PreCI CLI v2 已就绪 */
        READY,
        /** 找到 preci 但不是 v2 */
        OLD_VERSION,
        /** 未找到 preci 可执行文件 */
        NOT_FOUND
    }

    data class DetectionResult(
        val status: DetectionStatus,
        val path: String? = null,
        val message: String? = null
    )

    // ======================== 公共 API ========================

    /**
     * 检测 PreCI CLI 的安装状态和版本。
     *
     * 先通过 [resolvePreCIPath] 定位可执行文件，
     * 然后执行 `preci version` 并检查输出前 5 行是否包含 "PreCI CLI v2"。
     */
    fun detectPreCI(): DetectionResult {
        val path = resolvePreCIPath()
            ?: return DetectionResult(DetectionStatus.NOT_FOUND)

        return try {
            val process = ProcessBuilder(path)
                .redirectErrorStream(true)
                .start()
            val lines = process.inputStream.bufferedReader().readLines()
            val completed = process.waitFor(5, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return DetectionResult(DetectionStatus.OLD_VERSION, path, "version 命令执行超时")
            }

            val first5 = lines.take(5)
            val isV2 = first5.any { "PreCI CLI v2" in it }

            if (isV2) {
                persistPathIfNeeded(path)
                DetectionResult(DetectionStatus.READY, path)
            } else {
                DetectionResult(
                    DetectionStatus.OLD_VERSION, path,
                    first5.joinToString("\n").ifBlank { "无版本输出" }
                )
            }
        } catch (e: Exception) {
            logger.warn("Failed to run '$path version': ${e.message}")
            DetectionResult(DetectionStatus.OLD_VERSION, path, "无法执行: ${e.message}")
        }
    }

    /**
     * 解析 `preci` 可执行文件的绝对路径。
     *
     * 查找顺序：
     * 1. Settings 中用户手动配置的路径
     * 2. ~/PreCI 目录及其一级子目录
     * 3. 常见系统目录 + JVM 继承的 PATH
     *
     * @return 绝对路径，未找到返回 null
     */
    fun resolvePreCIPath(): String? {
        // 1. 用户在 Settings 中配置的路径
        try {
            val settings = PreCISettings.getInstance()
            val configured = settings.preciPath
            if (configured.isNotBlank()) {
                val file = File(configured)
                if (file.isFile && file.canExecute()) return configured
                logger.warn("Configured preciPath is not a valid executable: $configured")
            }
        } catch (e: Exception) {
            logger.debug("Could not read PreCISettings: ${e.message}")
        }

        // 2. 优先搜索 ~/PreCI
        val found = searchInPreCIHome()
        if (found != null) return found

        // 3. 常见目录 + 系统 PATH
        return resolveExecutable("preci")
    }

    /**
     * 创建 ProcessBuilder，对 `preci` 命令自动解析为绝对路径。
     */
    fun createProcessBuilder(vararg command: String): ProcessBuilder {
        if (isWindows()) {
            val resolvedCommand = command.toMutableList()
            if (command[0] == "preci") {
                resolvePreCIPath()?.let { resolvedCommand[0] = it }
            }
            val quoted = resolvedCommand.map { if (it.contains(" ")) "\"$it\"" else it }
            return ProcessBuilder("cmd", "/c", quoted.joinToString(" "))
        }

        val resolvedCommand = command.toMutableList()
        val resolved = if (command[0] == "preci") {
            resolvePreCIPath()
        } else {
            resolveExecutable(command[0])
        }

        if (resolved != null) {
            resolvedCommand[0] = resolved
            logger.debug("Resolved '${command[0]}' -> $resolved")
        } else {
            logger.warn("Could not resolve '${command[0]}' in search dirs, using as-is")
        }

        val pb = ProcessBuilder(resolvedCommand)
        pb.environment()["PATH"] = searchDirs.joinToString(File.pathSeparator)
        return pb
    }

    // ======================== 内部方法 ========================

    /**
     * 在 ~/PreCI 目录中搜索 preci 可执行文件（含一级子目录）
     */
    private fun searchInPreCIHome(): String? {
        val home = System.getProperty("user.home")
        val execName = if (isWindows()) "preci.exe" else "preci"
        val preciHome = File(home, "PreCI")

        if (!preciHome.isDirectory) return null

        val direct = File(preciHome, execName)
        if (direct.isFile && direct.canExecute()) return direct.absolutePath

        preciHome.listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { subDir ->
                val candidate = File(subDir, execName)
                if (candidate.isFile && candidate.canExecute()) return candidate.absolutePath
            }

        return null
    }

    private fun resolveExecutable(name: String): String? {
        if (name.contains(File.separator)) return name

        val suffixes = if (isWindows()) listOf(".exe", ".bat", ".cmd", "") else listOf("")
        for (dir in searchDirs) {
            for (suffix in suffixes) {
                val candidate = File(dir, name + suffix)
                if (candidate.isFile && candidate.canExecute()) {
                    return candidate.absolutePath
                }
            }
        }
        return null
    }

    private fun persistPathIfNeeded(path: String) {
        try {
            val settings = PreCISettings.getInstance()
            if (settings.preciPath.isBlank()) {
                settings.preciPath = path
                logger.info("Auto-detected PreCI path persisted to settings: $path")
            }
        } catch (e: Exception) {
            logger.debug("Could not persist detected path: ${e.message}")
        }
    }

    private fun buildSearchDirs(): List<String> {
        val home = System.getProperty("user.home")
        val currentPath = System.getenv("PATH")
            ?: if (isWindows()) "" else "/usr/bin:/bin"

        val extraDirs = mutableListOf(
            "$home/.preci/bin",
            "/usr/local/bin",
            "/opt/homebrew/bin",
            "/opt/homebrew/sbin",
            "$home/.local/bin",
            "$home/bin"
        )

        if (isMacOS()) {
            extraDirs.addAll(readMacOSSystemPaths())
        }

        val allDirs = (extraDirs + currentPath.split(File.pathSeparator))
            .filter { it.isNotBlank() }
            .distinct()

        logger.info("Executable search dirs: $allDirs")
        return allDirs
    }

    private fun readMacOSSystemPaths(): List<String> {
        val paths = mutableListOf<String>()
        try {
            val etcPaths = File("/etc/paths")
            if (etcPaths.exists()) {
                paths.addAll(etcPaths.readLines().filter { it.isNotBlank() })
            }
            val pathsDir = File("/etc/paths.d")
            if (pathsDir.isDirectory) {
                pathsDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        paths.addAll(file.readLines().filter { it.isNotBlank() })
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to read macOS system paths", e)
        }
        return paths
    }

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("win")

    private fun isMacOS(): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return os.contains("mac") || os.contains("darwin")
    }
}
