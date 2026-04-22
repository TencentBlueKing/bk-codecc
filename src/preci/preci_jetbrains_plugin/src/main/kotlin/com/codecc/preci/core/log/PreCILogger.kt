package com.codecc.preci.core.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * PreCI 统一日志工具类
 *
 * 基于 SLF4J + Logback 实现，提供强大而灵活的日志功能。
 *
 * **主要特性：**
 * - 完全自定义的输出位置（通过 logback.xml 配置）
 * - 支持异步写入，性能优秀
 * - 内置文件滚动和历史文件管理
 * - 通过配置文件灵活控制日志行为
 * - 线程安全
 *
 * **日志级别：**
 * - **TRACE**: 最详细的调试信息，通常用于追踪代码执行流程
 * - **DEBUG**: 调试信息，用于开发和问题排查
 * - **INFO**: 一般信息，记录关键操作和状态变化
 * - **WARN**: 警告信息，表示潜在问题但不影响功能
 * - **ERROR**: 错误信息，表示功能异常但程序可以继续运行
 *
 * **使用方式：**
 * ```kotlin
 * class MyService {
 *     private val logger = PreCILogger.getLogger(MyService::class.java)
 *
 *     fun doSomething() {
 *         logger.info("开始执行任务")
 *         logger.debug("调试信息: state={}", state)
 *         
 *         try {
 *             // 业务逻辑
 *             logger.info("任务执行成功")
 *         } catch (e: Exception) {
 *             logger.error("任务执行失败", e)
 *         }
 *     }
 * }
 * ```
 *
 * **配置说明：**
 * - 日志配置文件：`src/main/resources/logback.xml`
 * - 日志级别可通过系统属性 `preci.log.level` 控制
 * - 日志路径可通过系统属性 `preci.log.path` 控制
 * - 默认日志路径：`${user.home}/.preci/logs/preci.log`
 *
 * **参数化日志：**
 * ```kotlin
 * // 推荐：使用参数化（避免不必要的字符串拼接）
 * logger.debug("用户 {} 执行操作 {}", userId, operation)
 *
 * // 不推荐：字符串拼接（总是会执行拼接，即使日志级别不输出）
 * logger.debug("用户 $userId 执行操作 $operation")
 * ```
 *
 * **性能考虑：**
 * - 日志采用异步写入，不会阻塞主线程
 * - 使用参数化日志避免不必要的字符串拼接
 * - 在性能敏感的代码中，使用 `isDebugEnabled()` 进行条件检查
 *
 * @property logger SLF4J Logger 实例
 * @since 2.0
 * @see org.slf4j.Logger
 */
@Suppress("TooManyFunctions") // 日志工具类需要为每个日志级别提供多个重载方法
class PreCILogger private constructor(
    private val logger: Logger
) {
    /**
     * 记录 TRACE 级别日志
     *
     * TRACE 日志用于记录最详细的调试信息，通常用于追踪代码执行流程。
     * 只有在日志级别设置为 TRACE 时才会记录。
     *
     * @param message 日志消息
     */
    fun trace(message: String) {
        logger.trace(message)
    }

    /**
     * 记录 TRACE 级别日志（参数化）
     *
     * 使用参数化可以避免不必要的字符串拼接，提升性能。
     *
     * @param message 日志消息模板（使用 {} 作为占位符）
     * @param args 参数列表
     */
    fun trace(message: String, vararg args: Any?) {
        logger.trace(message, *args)
    }

    /**
     * 记录 TRACE 级别日志（带异常）
     *
     * @param message 日志消息
     * @param throwable 异常对象
     */
    fun trace(message: String, throwable: Throwable) {
        logger.trace(message, throwable)
    }

    /**
     * 记录 DEBUG 级别日志
     *
     * DEBUG 日志用于记录调试信息，便于开发和问题排查。
     * 只有在日志级别设置为 DEBUG 或更低时才会记录。
     *
     * @param message 日志消息
     */
    fun debug(message: String) {
        logger.debug(message)
    }

    /**
     * 记录 DEBUG 级别日志（参数化）
     *
     * @param message 日志消息模板（使用 {} 作为占位符）
     * @param args 参数列表
     */
    fun debug(message: String, vararg args: Any?) {
        logger.debug(message, *args)
    }

    /**
     * 记录 DEBUG 级别日志（带异常）
     *
     * @param message 日志消息
     * @param throwable 异常对象
     */
    fun debug(message: String, throwable: Throwable) {
        logger.debug(message, throwable)
    }

    /**
     * 记录 INFO 级别日志
     *
     * INFO 日志用于记录一般信息，如关键操作和状态变化。
     * INFO 日志在生产环境中也会被记录。
     *
     * @param message 日志消息
     */
    fun info(message: String) {
        logger.info(message)
    }

    /**
     * 记录 INFO 级别日志（参数化）
     *
     * @param message 日志消息模板（使用 {} 作为占位符）
     * @param args 参数列表
     */
    fun info(message: String, vararg args: Any?) {
        logger.info(message, *args)
    }

    /**
     * 记录 INFO 级别日志（带异常）
     *
     * @param message 日志消息
     * @param throwable 异常对象
     */
    fun info(message: String, throwable: Throwable) {
        logger.info(message, throwable)
    }

    /**
     * 记录 WARN 级别日志
     *
     * WARN 日志用于记录警告信息，表示潜在问题但不影响功能。
     *
     * @param message 日志消息
     */
    fun warn(message: String) {
        logger.warn(message)
    }

    /**
     * 记录 WARN 级别日志（参数化）
     *
     * @param message 日志消息模板（使用 {} 作为占位符）
     * @param args 参数列表
     */
    fun warn(message: String, vararg args: Any?) {
        logger.warn(message, *args)
    }

    /**
     * 记录 WARN 级别日志（带异常）
     *
     * @param message 日志消息
     * @param throwable 异常对象
     */
    fun warn(message: String, throwable: Throwable) {
        logger.warn(message, throwable)
    }

    /**
     * 记录 ERROR 级别日志
     *
     * ERROR 日志用于记录错误信息，表示功能异常但程序可以继续运行。
     *
     * @param message 日志消息
     */
    fun error(message: String) {
        logger.error(message)
    }

    /**
     * 记录 ERROR 级别日志（参数化）
     *
     * @param message 日志消息模板（使用 {} 作为占位符）
     * @param args 参数列表
     */
    fun error(message: String, vararg args: Any?) {
        logger.error(message, *args)
    }

    /**
     * 记录 ERROR 级别日志（带异常）
     *
     * @param message 日志消息
     * @param throwable 异常对象
     */
    fun error(message: String, throwable: Throwable) {
        logger.error(message, throwable)
    }

    /**
     * 检查是否启用 DEBUG 级别
     *
     * 在性能敏感的代码中，可以使用此方法进行条件检查，
     * 避免不必要的字符串构建或对象创建。
     *
     * **使用示例：**
     * ```kotlin
     * if (logger.isDebugEnabled()) {
     *     val complexData = buildComplexDebugInfo() // 只在 DEBUG 模式下才执行
     *     logger.debug("复杂数据：$complexData")
     * }
     * ```
     *
     * @return true 如果 DEBUG 级别启用，否则返回 false
     */
    fun isDebugEnabled(): Boolean = logger.isDebugEnabled

    /**
     * 检查是否启用 TRACE 级别
     *
     * @return true 如果 TRACE 级别启用，否则返回 false
     */
    fun isTraceEnabled(): Boolean = logger.isTraceEnabled

    /**
     * 检查是否启用 INFO 级别
     *
     * @return true 如果 INFO 级别启用，否则返回 false
     */
    fun isInfoEnabled(): Boolean = logger.isInfoEnabled

    /**
     * 检查是否启用 WARN 级别
     *
     * @return true 如果 WARN 级别启用，否则返回 false
     */
    fun isWarnEnabled(): Boolean = logger.isWarnEnabled

    /**
     * 检查是否启用 ERROR 级别
     *
     * @return true 如果 ERROR 级别启用，否则返回 false
     */
    fun isErrorEnabled(): Boolean = logger.isErrorEnabled

    companion object {
        /**
         * 获取指定类的 Logger 实例
         *
         * 每个类应该使用自己的 Logger 实例，便于在日志中区分来源。
         *
         * **使用示例：**
         * ```kotlin
         * class MyService {
         *     companion object {
         *         private val logger = PreCILogger.getLogger(MyService::class.java)
         *     }
         *     
         *     // 或者作为实例属性
         *     private val logger = PreCILogger.getLogger(MyService::class.java)
         * }
         * ```
         *
         * @param clazz 使用 Logger 的类
         * @return PreCILogger 实例
         */
        fun getLogger(clazz: Class<*>): PreCILogger {
            return PreCILogger(LoggerFactory.getLogger(clazz))
        }

        /**
         * 获取指定名称的 Logger 实例
         *
         * 可以使用自定义的名称而不是类名，用于更灵活的日志分类。
         *
         * **使用示例：**
         * ```kotlin
         * val logger = PreCILogger.getLogger("PreCI.Network")
         * val httpLogger = PreCILogger.getLogger("PreCI.HTTP")
         * ```
         *
         * @param name 日志名称
         * @return PreCILogger 实例
         */
        fun getLogger(name: String): PreCILogger {
            return PreCILogger(LoggerFactory.getLogger(name))
        }
    }
}
