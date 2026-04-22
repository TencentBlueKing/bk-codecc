package com.codecc.preci

import com.codecc.preci.core.log.LogbackConfigurator
import org.junit.jupiter.api.BeforeAll

/**
 * 测试基类
 *
 * 提供测试环境的通用初始化逻辑，包括日志配置初始化。
 * 所有测试类可以继承此类以获得标准的测试环境。
 *
 * **功能：**
 * - 初始化 Logback 日志配置
 * - 确保测试中的日志能够正常输出到控制台
 *
 * **使用方式：**
 * ```kotlin
 * class MyServiceTest : BaseTest() {
 *     @Test
 *     fun testSomething() {
 *         // 测试代码，日志会正常输出
 *     }
 * }
 * ```
 *
 * @since 2.0
 */
open class BaseTest {
    
    companion object {
        /**
         * 标记是否已初始化
         */
        @Volatile
        private var initialized = false
        
        /**
         * 在所有测试前执行一次
         *
         * 初始化 Logback 配置，使测试中的日志能够正常输出。
         * LogbackConfigurator 会自动检测测试环境并使用合适的默认配置。
         */
        @JvmStatic
        @BeforeAll
        fun setupLogging() {
            if (!initialized) {
                synchronized(this) {
                    if (!initialized) {
                        // 初始化 Logback 配置
                        // 注意：测试环境会自动加载 logback-test.xml
                        // LogbackConfigurator 会检测 Application 是否可用，并使用合适的配置
                        LogbackConfigurator.initialize()
                        initialized = true
                    }
                }
            }
        }
    }
}

