package com.codecc.preci.core.log

import com.codecc.preci.BaseTest
import com.codecc.preci.core.config.PreCISettings
import org.junit.Assert
import java.io.File

/**
 * PreCILogger 单元测试
 *
 * 测试基于 SLF4J + Logback 的日志功能：
 * - 日志级别检查
 * - 参数化日志
 * - 异常日志
 * - 日志文件输出
 *
 * @since 2.0
 */
class PreCILoggerTest : BaseTest() {

    private lateinit var logger: PreCILogger

    /**
     * 测试获取 Logger 实例
     */
    @org.junit.jupiter.api.Test
    fun testGetLogger() {
        val logger1 = PreCILogger.getLogger(PreCILoggerTest::class.java)
        val logger2 = PreCILogger.getLogger("TestLogger")
        
        Assert.assertNotNull("Logger 实例不应该为 null", logger1)
        Assert.assertNotNull("Logger 实例不应该为 null", logger2)
    }

    /**
     * 测试 INFO 级别日志
     */
    @org.junit.jupiter.api.Test
    fun testInfoLog() {
        logger = PreCILogger.getLogger(PreCILoggerTest::class.java)
        logger.info("测试 INFO 日志")
        // INFO 日志总是启用
        Assert.assertTrue("INFO 级别应该启用", logger.isInfoEnabled())
    }

    /**
     * 测试参数化日志
     */
    @org.junit.jupiter.api.Test
    fun testParameterizedLog() {
        logger = PreCILogger.getLogger(PreCILoggerTest::class.java)
        val userId = "user123"
        val operation = "scan"
        
        logger.info("用户 {} 执行操作 {}", userId, operation)
        logger.debug("调试信息: key={}, value={}", "test", 123)
        
        // 验证日志方法不会抛出异常
        Assert.assertTrue(true)
    }

    /**
     * 测试异常日志
     */
    @org.junit.jupiter.api.Test
    fun testExceptionLog() {
        logger = PreCILogger.getLogger(PreCILoggerTest::class.java)
        val exception = RuntimeException("测试异常")

        logger.error("发生错误", exception)
        logger.warn("警告信息", exception)
        logger.debug("调试异常", exception)
        
        // 验证日志方法不会抛出异常
        Assert.assertTrue(true)
    }

    /**
     * 测试日志级别检查
     */
    @org.junit.jupiter.api.Test
    fun testLogLevelCheck() {
        logger = PreCILogger.getLogger(PreCILoggerTest::class.java)
        // INFO 和以上级别总是启用
        Assert.assertTrue("INFO 应该启用", logger.isInfoEnabled())
        Assert.assertTrue("WARN 应该启用", logger.isWarnEnabled())
        Assert.assertTrue("ERROR 应该启用", logger.isErrorEnabled())
        
        // DEBUG 和 TRACE 取决于配置
        val settings = PreCISettings.getInstance()
        val debugEnabled = settings.enableDebugLog
        
        if (debugEnabled) {
            Assert.assertTrue("DEBUG 应该启用（配置已启用）", logger.isDebugEnabled())
        }
    }

    /**
     * 测试所有日志级别
     */
    @org.junit.jupiter.api.Test
    fun testAllLogLevels() {
        logger = PreCILogger.getLogger(PreCILoggerTest::class.java)
        logger.trace("TRACE 消息")
        logger.trace("TRACE {} 消息", "参数化")
        
        logger.debug("DEBUG 消息")
        logger.debug("DEBUG {} 消息", "参数化")
        
        logger.info("INFO 消息")
        logger.info("INFO {} 消息", "参数化")
        
        logger.warn("WARN 消息")
        logger.warn("WARN {} 消息", "参数化")
        
        logger.error("ERROR 消息")
        logger.error("ERROR {} 消息", "参数化")
        
        // 验证所有日志方法都能正常调用
        Assert.assertTrue(true)
    }

    /**
     * 测试多参数日志
     */
    @org.junit.jupiter.api.Test
    fun testMultipleParameters() {
        logger = PreCILogger.getLogger(PreCILoggerTest::class.java)
        logger.info("多参数日志: {} {} {} {}", "参数1", "参数2", 123, true)
        logger.debug("调试多参数: a={}, b={}, c={}", 1, 2, 3)
        
        // 验证多参数日志不会抛出异常
        Assert.assertTrue(true)
    }

    /**
     * 测试 null 参数
     */
    @org.junit.jupiter.api.Test
    fun testNullParameters() {
        logger = PreCILogger.getLogger(PreCILoggerTest::class.java)
        logger.info("null 参数: {}", null as Any?)
        logger.debug("多个参数包含 null: {} {} {}", "normal", null, "value")
        
        // 验证 null 参数不会导致异常
        Assert.assertTrue(true)
    }

    /**
     * 测试 Logger 名称
     */
    @org.junit.jupiter.api.Test
    fun testLoggerName() {
        val logger1 = PreCILogger.getLogger(PreCILoggerTest::class.java)
        val logger2 = PreCILogger.getLogger("CustomLogger")
        
        logger1.info("来自类名 Logger 的日志")
        logger2.info("来自自定义名称 Logger 的日志")
        
        // 验证不同 Logger 都能正常工作
        Assert.assertTrue(true)
    }
}
