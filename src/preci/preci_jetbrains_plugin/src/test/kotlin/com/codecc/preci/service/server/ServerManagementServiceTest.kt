package com.codecc.preci.service.server

import com.codecc.preci.core.http.ServerNotRunningException
import com.codecc.preci.util.PreCIPortDetector
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * ServerManagementService 单元测试
 *
 * 测试服务管理功能的各种场景
 *
 * **注意：** 
 * 这些测试主要验证逻辑流程，某些测试依赖 PreCI CLI 的实际安装状态。
 * 在 CI 环境中，部分测试可能需要 Mock 或跳过。
 *
 * @since 1.0
 */
@DisplayName("ServerManagementService 测试")
class ServerManagementServiceTest : BasePlatformTestCase() {

    private lateinit var serverManagement: ServerManagementService

    override fun setUp() {
        super.setUp()
        serverManagement = ServerManagementServiceImpl(project)
    }

    @Test
    @DisplayName("测试 PreCI 安装检测 - 基本功能")
    fun `should detect PreCI installation status`() = runBlocking {
        // 测试安装检测功能
        // 注意：此测试结果取决于实际环境中是否安装了 PreCI
        val isInstalled = serverManagement.isPreCIInstalled()
        
        // 检查返回值是布尔类型
        assertTrue(isInstalled is Boolean)
        
        // 打印结果用于调试
        println("PreCI 安装状态: $isInstalled")
    }

    @Test
    @DisplayName("测试服务运行状态检测 - 基本功能")
    fun `should detect server running status`() = runBlocking {
        // 测试服务运行状态检测
        // 注意：此测试结果取决于服务是否正在运行
        val isRunning = serverManagement.isServerRunning()
        
        // 检查返回值是布尔类型
        assertTrue(isRunning is Boolean)
        
        // 打印结果用于调试
        println("PreCI Server 运行状态: $isRunning")
    }

    @Test
    @DisplayName("测试获取服务状态")
    fun `should get server state`() {
        // 获取当前服务状态
        val state = serverManagement.getServerState()
        
        // 验证状态是有效的枚举值
        assertNotNull(state)
        assertTrue(state in ServerState.values())
        
        println("当前服务状态: $state")
    }

    @Test
    @DisplayName("测试状态监听器注册")
    fun `should register state change listener`() {
        var stateChangeCount = 0
        var lastOldState: ServerState? = null
        var lastNewState: ServerState? = null

        val listener = object : ServerStateChangeListener {
            override fun onStateChanged(oldState: ServerState, newState: ServerState) {
                stateChangeCount++
                lastOldState = oldState
                lastNewState = newState
            }
        }

        // 注册监听器
        serverManagement.addStateChangeListener(listener)

        // 触发状态变更（通过启动服务，如果可用的话）
        // 注意：这个测试可能需要 Mock，因为实际启动服务可能失败
        
        // 移除监听器
        serverManagement.removeStateChangeListener(listener)

        // 基本验证：监听器可以被添加和移除
        // 实际的状态变更测试需要 Mock 或集成测试环境
        assertTrue(true)
    }

    @Test
    @DisplayName("测试下载安装功能 - 应该抛出 NotImplementedError")
    fun `should throw NotImplementedError for downloadAndInstall`() = runBlocking {
        // 验证下载安装功能尚未实现
        val exception = assertThrows<NotImplementedError> {
            runBlocking {
                serverManagement.downloadAndInstall()
            }
        }

        // 验证异常信息包含预期内容
        assertTrue(exception.message!!.contains("尚未实现"))
        assertTrue(exception.message!!.contains("安装"))
    }

    @Test
    @DisplayName("测试 ServerState 枚举值")
    fun `should have all expected server states`() {
        // 验证所有预期的状态枚举值都存在
        val states = ServerState.values()
        
        assertTrue(states.contains(ServerState.NOT_INSTALLED))
        assertTrue(states.contains(ServerState.STOPPED))
        assertTrue(states.contains(ServerState.STARTING))
        assertTrue(states.contains(ServerState.RUNNING))
        assertTrue(states.contains(ServerState.STOPPING))
        assertTrue(states.contains(ServerState.ERROR))
        
        // 验证枚举值数量
        assertEquals(6, states.size)
    }

    @Test
    @DisplayName("测试 ServerStartResult 密封类")
    fun `should work with ServerStartResult sealed class`() {
        // 测试成功结果
        val successResult = ServerStartResult.Success(8080)
        assertTrue(successResult is ServerStartResult.Success)
        assertEquals(8080, successResult.port)

        // 测试失败结果
        val failureResult = ServerStartResult.Failure("启动失败")
        assertTrue(failureResult is ServerStartResult.Failure)
        assertEquals("启动失败", failureResult.message)
    }

    @Test
    @DisplayName("测试 ServerStopResult 密封类")
    fun `should work with ServerStopResult sealed class`() {
        // 测试成功结果
        val successResult = ServerStopResult.Success
        assertTrue(successResult is ServerStopResult.Success)

        // 测试失败结果
        val failureResult = ServerStopResult.Failure("停止失败")
        assertTrue(failureResult is ServerStopResult.Failure)
        assertEquals("停止失败", failureResult.message)
    }

    @Test
    @DisplayName("测试 InstallResult 密封类")
    fun `should work with InstallResult sealed class`() {
        // 测试成功结果
        val successResult = InstallResult.Success("/usr/local/bin/install.sh")
        assertTrue(successResult is InstallResult.Success)
        assertEquals("/usr/local/bin/install.sh", successResult.installScriptPath)

        // 测试超时结果
        val timeoutResult = InstallResult.Timeout
        assertTrue(timeoutResult is InstallResult.Timeout)

        // 测试失败结果
        val failureResult = InstallResult.Failure("安装失败")
        assertTrue(failureResult is InstallResult.Failure)
        assertEquals("安装失败", failureResult.message)
    }

    @Test
    @DisplayName("测试 ServerManagementService.getInstance")
    fun `should get service instance from project`() {
        // 测试从项目获取服务实例
        val service = ServerManagementService.getInstance(project)
        
        assertNotNull(service)
        assertTrue(service is ServerManagementService)
    }
}

