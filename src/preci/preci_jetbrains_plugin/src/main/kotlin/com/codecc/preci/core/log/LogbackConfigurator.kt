package com.codecc.preci.core.log

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import com.codecc.preci.core.config.PreCISettings
import org.slf4j.LoggerFactory

/**
 * Logback 配置管理器
 *
 * 负责根据插件配置动态调整 Logback 的行为，包括：
 * - 日志级别
 * - 日志文件路径
 * - 其他运行时配置
 *
 * **使用方式：**
 * ```kotlin
 * // 初始化 Logback 配置（在插件启动时调用）
 * LogbackConfigurator.initialize()
 *
 * // 更新日志级别（当用户修改配置时调用）
 * LogbackConfigurator.updateLogLevel(enableDebug = true)
 * ```
 *
 * **注意事项：**
 * - 日志路径的更改需要重启 IDE 才能生效
 * - 日志级别可以动态更新，无需重启
 *
 * @since 2.0
 */
object LogbackConfigurator {

    private val logger = PreCILogger.getLogger(LogbackConfigurator::class.java)

    /**
     * 获取 Logback 上下文
     */
    private fun getLoggerContext(): LoggerContext {
        return LoggerFactory.getILoggerFactory() as LoggerContext
    }

    /**
     * 初始化 Logback 配置
     *
     * 从插件配置读取日志相关设置，并更新 Logback 配置。
     * 此方法应该在插件启动时调用一次。
     *
     * **配置内容：**
     * - 日志路径：从 `PreCISettings.customLogPath` 读取
     * - 日志级别：从 `PreCISettings.enableDebugLog` 读取
     *
     * **测试环境：**
     * - 如果 IntelliJ Application 未初始化（测试环境），使用默认配置
     * - 默认路径：${PRECI_HOME}/log 或 ${user.home}/PreCI/log
     * - 默认级别：DEBUG
     */
    @Suppress("TooGenericExceptionCaught") // 配置初始化需要捕获所有异常以保证健壮性
    fun initialize() {
        try {
            val context = getLoggerContext()

            // 检查是否在测试环境（Application 未初始化）
            val application = com.intellij.openapi.application.ApplicationManager.getApplication()
            
            if (application == null || application.isDisposed) {
                // 测试环境或 Application 已销毁，使用默认配置
                val logPath = determineDefaultLogPath()
                context.putProperty("preci.log.path", logPath)
                context.putProperty("preci.log.level", "DEBUG")
                logger.info("Logback 配置初始化完成（测试环境）：级别=DEBUG, 路径={}", logPath)
                return
            }

            // 生产环境，从 PreCISettings 读取配置
            val settings = PreCISettings.getInstance()

            // 设置日志路径
            val logPath = determineLogPath(settings)
            context.putProperty("preci.log.path", logPath)

            // 设置日志级别
            val logLevel = if (settings.enableDebugLog) "DEBUG" else "INFO"
            context.putProperty("preci.log.level", logLevel)
            updateLogLevel(settings.enableDebugLog)

            logger.info("Logback 配置初始化完成：级别={}, 路径={}", logLevel, logPath)
            logger.info("日志文件位置: {}/preci-jbplugin.log", logPath)
            
            // 也输出到控制台，便于开发调试
            println("PreCI 日志文件位置: $logPath/preci-jbplugin.log")
        } catch (e: Exception) {
            // 配置初始化失败不应影响程序运行，使用默认配置
            System.err.println("Logback 配置初始化失败，使用默认配置: ${e.message}")
            
            // 设置默认配置
            try {
                val context = getLoggerContext()
                val logPath = determineDefaultLogPath()
                context.putProperty("preci.log.path", logPath)
                context.putProperty("preci.log.level", "INFO")
            } catch (ex: Exception) {
                // 忽略，使用 Logback 内置默认值
            }
        }
    }

    /**
     * 更新日志级别
     *
     * 动态更新日志级别，无需重启 IDE。
     *
     * @param enableDebug 是否启用 DEBUG 级别
     */
    @Suppress("TooGenericExceptionCaught") // 配置更新需要捕获所有异常以保证健壮性
    fun updateLogLevel(enableDebug: Boolean) {
        try {
            val context = getLoggerContext()
            val logLevel = if (enableDebug) Level.DEBUG else Level.INFO

            // 更新系统属性
            context.putProperty("preci.log.level", logLevel.levelStr)

            // 更新根 Logger 级别
            val rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
            rootLogger.level = logLevel

            // 更新 PreCI 包的日志级别
            val preciLogger = context.getLogger("com.codecc.preci")
            preciLogger.level = logLevel

            logger.info("日志级别已更新为: {}", logLevel.levelStr)
        } catch (e: Exception) {
            System.err.println("更新日志级别失败: ${e.message}")
        }
    }

    /**
     * 更新日志路径
     *
     * 设置新的日志文件路径。
     *
     * **注意：** 路径更改需要重启 IDE 才能生效，因为文件 Appender 在启动时就已经打开了文件。
     *
     * @param path 新的日志路径（空字符串表示使用默认路径）
     */
    @Suppress("TooGenericExceptionCaught") // 配置更新需要捕获所有异常以保证健壮性
    fun updateLogPath(path: String) {
        try {
            val context = getLoggerContext()
            val actualPath = if (path.isBlank()) {
                val preciHome = System.getenv("PRECI_HOME")
                if (preciHome != null) {
                    "$preciHome/log"
                } else {
                    "${System.getProperty("user.home")}/PreCI/log"
                }
            } else {
                path
            }

            context.putProperty("preci.log.path", actualPath)
            logger.warn("日志路径已更新为: {}（需要重启 IDE 才能生效）", actualPath)
        } catch (e: Exception) {
            System.err.println("更新日志路径失败: ${e.message}")
        }
    }

    /**
     * 确定日志路径
     *
     * 根据配置决定使用自定义路径还是默认路径。
     *
     * @param settings 配置对象
     * @return 日志路径
     */
    private fun determineLogPath(settings: PreCISettings): String {
        // 首先检查环境变量 PRECI_HOME
        val preciHome = System.getenv("PRECI_HOME")
        
        return if (settings.customLogPath.isNotBlank()) {
            // 使用用户配置的自定义路径
            settings.customLogPath
        } else if (preciHome != null) {
            // 使用环境变量 PRECI_HOME
            "$preciHome/log"
        } else {
            // 使用默认路径：${user.home}/PreCI/log
            "${System.getProperty("user.home")}/PreCI/log"
        }
    }

    /**
     * 确定默认日志路径（不依赖 PreCISettings）
     *
     * 用于测试环境或配置服务不可用时。
     *
     * @return 默认日志路径
     */
    private fun determineDefaultLogPath(): String {
        val preciHome = System.getenv("PRECI_HOME")
        return if (preciHome != null) {
            "$preciHome/log"
        } else {
            "${System.getProperty("user.home")}/PreCI/log"
        }
    }

    /**
     * 获取当前日志级别
     *
     * @return 当前日志级别字符串（如 "DEBUG"、"INFO"）
     */
    fun getCurrentLogLevel(): String {
        return try {
            val context = getLoggerContext()
            val preciLogger = context.getLogger("com.codecc.preci")
            preciLogger.level?.levelStr ?: "INFO"
        } catch (e: Exception) {
            "INFO"
        }
    }

    /**
     * 获取当前日志路径
     *
     * @return 当前日志路径
     */
    fun getCurrentLogPath(): String {
        return try {
            val context = getLoggerContext()
            val path = context.getProperty("preci.log.path")
            if (path != null) {
                path
            } else {
                val preciHome = System.getenv("PRECI_HOME")
                if (preciHome != null) {
                    "$preciHome/log"
                } else {
                    "${System.getProperty("user.home")}/PreCI/log"
                }
            }
        } catch (e: Exception) {
            val preciHome = System.getenv("PRECI_HOME")
            if (preciHome != null) {
                "$preciHome/log"
            } else {
                "${System.getProperty("user.home")}/PreCI/log"
            }
        }
    }

    /**
     * 重置 Logback 配置为默认值
     *
     * 此方法会重新加载 logback.xml 配置文件。
     */
    @Suppress("TooGenericExceptionCaught") // 配置重置需要捕获所有异常以保证健壮性
    fun reset() {
        try {
            val context = getLoggerContext()
            context.reset()

            // 重新初始化
            initialize()

            logger.info("Logback 配置已重置为默认值")
        } catch (e: Exception) {
            System.err.println("重置 Logback 配置失败: ${e.message}")
        }
    }
}

