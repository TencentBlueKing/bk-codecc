package com.codecc.preci

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * 基础设施验证测试
 *
 * 用于验证项目构建和测试框架配置是否正确
 */
@DisplayName("基础设施验证测试")
class InfrastructureTest {

    @Test
    @DisplayName("验证 JUnit 5 测试框架")
    fun `should verify JUnit 5 is working`() {
        val expected = "PreCI"
        val actual = "PreCI"
        assertEquals(expected, actual, "JUnit 5 断言应该正常工作")
    }

    @Test
    @DisplayName("验证 Kotlin 基础功能")
    fun `should verify Kotlin features`() {
        // 验证数据类
        data class TestData(val name: String, val version: Int)
        val data = TestData("PreCI", 1)
        assertEquals("PreCI", data.name)
        assertEquals(1, data.version)

        // 验证扩展函数
        fun String.isPreCI() = this == "PreCI"
        assertTrue("PreCI".isPreCI())

        // 验证 Lambda
        val numbers = listOf(1, 2, 3, 4, 5)
        val doubled = numbers.map { it * 2 }
        assertEquals(listOf(2, 4, 6, 8, 10), doubled)
    }

    @Test
    @DisplayName("验证字符串操作")
    fun `should handle string operations`() {
        val text = "PreCI JetBrains Plugin"
        assertTrue(text.contains("PreCI"))
        assertTrue(text.startsWith("PreCI"))
        assertEquals(22, text.length)
    }

    @Test
    @DisplayName("验证集合操作")
    fun `should handle collection operations`() {
        val list = listOf(1, 2, 3, 4, 5)
        
        // filter
        val evens = list.filter { it % 2 == 0 }
        assertEquals(listOf(2, 4), evens)
        
        // map
        val squared = list.map { it * it }
        assertEquals(listOf(1, 4, 9, 16, 25), squared)
        
        // reduce
        val sum = list.reduce { acc, i -> acc + i }
        assertEquals(15, sum)
    }
}


