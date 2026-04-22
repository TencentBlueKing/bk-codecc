package com.codecc.preci.service.version

import com.codecc.preci.BaseTest
import com.codecc.preci.api.client.PreCIApiClient
import com.codecc.preci.api.model.response.LatestVersionResponse
import com.codecc.preci.core.http.BusinessException
import com.codecc.preci.core.http.NetworkException
import com.codecc.preci.util.ShellCommandHelper
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

/**
 * VersionServiceImpl 单元测试
 *
 * 测试版本服务的核心功能：
 * 1. 获取当前版本（`preci version`）
 * 2. 获取最新版本（`GET /misc/latestVersion`）
 * 3. 检查更新（版本比较逻辑）
 * 4. 执行更新（`preci update`）
 *
 * **Mock 策略：**
 * - [ShellCommandHelper] 的 `createProcessBuilder` 被 mock，避免实际执行 CLI 命令
 * - [PreCIApiClient] 的 `getLatestVersion` 被 mock，避免实际网络请求
 * - 通过 spy 模式对 [VersionServiceImpl] 的内部命令执行方法进行 mock
 *
 * @since 2.0
 */
class VersionServiceTest : BaseTest() {

    private lateinit var versionService: VersionServiceImpl
    private lateinit var apiClient: PreCIApiClient

    @BeforeEach
    fun setUp() {
        apiClient = mockk(relaxed = true)

        versionService = spyk(VersionServiceImpl())

        mockkObject(ShellCommandHelper)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // ========== getCurrentVersion 测试 ==========

    /**
     * 测试：成功获取当前版本
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `getCurrentVersion should return success when command succeeds`() = runBlocking {
        every { versionService.executeVersionCommand() } returns "1.2.3"

        val result = versionService.getCurrentVersion()

        assertTrue(result is VersionResult.Success)
        assertEquals("1.2.3", (result as VersionResult.Success).version)
    }

    /**
     * 测试：获取当前版本命令执行失败时应返回 Failure
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `getCurrentVersion should return failure when command fails`() = runBlocking {
        every { versionService.executeVersionCommand() } throws RuntimeException("command not found")

        val result = versionService.getCurrentVersion()

        assertTrue(result is VersionResult.Failure)
        assertTrue((result as VersionResult.Failure).message.contains("command not found"))
    }

    /**
     * 测试：获取当前版本命令超时时应返回 Failure
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `getCurrentVersion should return failure when command times out`() = runBlocking {
        every { versionService.executeVersionCommand() } throws RuntimeException("preci version 命令执行超时（10s）")

        val result = versionService.getCurrentVersion()

        assertTrue(result is VersionResult.Failure)
        assertTrue((result as VersionResult.Failure).message.contains("超时"))
    }

    // ========== getLatestVersion 测试 ==========

    /**
     * 测试：成功获取最新版本
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `getLatestVersion should return success when API call succeeds`() = runBlocking {
        val mockApiClient = mockk<PreCIApiClient>()
        coEvery { mockApiClient.getLatestVersion() } returns LatestVersionResponse("2.0.0")

        val service = spyk(VersionServiceImpl())
        val apiClientField = VersionServiceImpl::class.java.getDeclaredField("apiClient")
        apiClientField.isAccessible = true
        apiClientField.set(service, mockApiClient)

        val result = service.getLatestVersion()

        assertTrue(result is VersionResult.Success)
        assertEquals("2.0.0", (result as VersionResult.Success).version)
    }

    /**
     * 测试：API 网络错误时获取最新版本应返回 Failure
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `getLatestVersion should return failure on network error`() = runBlocking {
        val mockApiClient = mockk<PreCIApiClient>()
        coEvery { mockApiClient.getLatestVersion() } throws NetworkException("Connection refused")

        val service = spyk(VersionServiceImpl())
        val apiClientField = VersionServiceImpl::class.java.getDeclaredField("apiClient")
        apiClientField.isAccessible = true
        apiClientField.set(service, mockApiClient)

        val result = service.getLatestVersion()

        assertTrue(result is VersionResult.Failure)
        assertTrue((result as VersionResult.Failure).message.contains("网络错误"))
    }

    /**
     * 测试：API 业务异常时获取最新版本应返回 Failure
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `getLatestVersion should return failure on business error`() = runBlocking {
        val mockApiClient = mockk<PreCIApiClient>()
        coEvery { mockApiClient.getLatestVersion() } throws BusinessException(500, "Internal Server Error")

        val service = spyk(VersionServiceImpl())
        val apiClientField = VersionServiceImpl::class.java.getDeclaredField("apiClient")
        apiClientField.isAccessible = true
        apiClientField.set(service, mockApiClient)

        val result = service.getLatestVersion()

        assertTrue(result is VersionResult.Failure)
        assertTrue((result as VersionResult.Failure).message.contains("获取最新版本失败"))
    }

    // ========== checkForUpdate 测试 ==========

    /**
     * 测试：版本相同时应返回 AlreadyLatest
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `checkForUpdate should return AlreadyLatest when versions match`() = runBlocking {
        coEvery { versionService.getCurrentVersion() } returns VersionResult.Success("1.0.0")
        coEvery { versionService.getLatestVersion() } returns VersionResult.Success("1.0.0")

        val result = versionService.checkForUpdate()

        assertTrue(result is UpdateCheckResult.AlreadyLatest)
        assertEquals("1.0.0", (result as UpdateCheckResult.AlreadyLatest).currentVersion)
    }

    /**
     * 测试：版本不同时应返回 UpdateAvailable
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `checkForUpdate should return UpdateAvailable when versions differ`() = runBlocking {
        coEvery { versionService.getCurrentVersion() } returns VersionResult.Success("1.0.0")
        coEvery { versionService.getLatestVersion() } returns VersionResult.Success("2.0.0")

        val result = versionService.checkForUpdate()

        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val updateResult = result as UpdateCheckResult.UpdateAvailable
        assertEquals("1.0.0", updateResult.currentVersion)
        assertEquals("2.0.0", updateResult.latestVersion)
    }

    /**
     * 测试：带 "v" 前缀的版本号应正确比较（v1.0.0 == 1.0.0）
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `checkForUpdate should handle v-prefix in version strings`() = runBlocking {
        coEvery { versionService.getCurrentVersion() } returns VersionResult.Success("v1.0.0")
        coEvery { versionService.getLatestVersion() } returns VersionResult.Success("1.0.0")

        val result = versionService.checkForUpdate()

        assertTrue(result is UpdateCheckResult.AlreadyLatest)
    }

    /**
     * 测试：获取当前版本失败时 checkForUpdate 应返回 Failure
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `checkForUpdate should return failure when getCurrentVersion fails`() = runBlocking {
        coEvery { versionService.getCurrentVersion() } returns VersionResult.Failure("命令未找到")

        val result = versionService.checkForUpdate()

        assertTrue(result is UpdateCheckResult.Failure)
        assertTrue((result as UpdateCheckResult.Failure).message.contains("当前版本"))
    }

    /**
     * 测试：获取最新版本失败时 checkForUpdate 应返回 Failure
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `checkForUpdate should return failure when getLatestVersion fails`() = runBlocking {
        coEvery { versionService.getCurrentVersion() } returns VersionResult.Success("1.0.0")
        coEvery { versionService.getLatestVersion() } returns VersionResult.Failure("网络错误")

        val result = versionService.checkForUpdate()

        assertTrue(result is UpdateCheckResult.Failure)
        assertTrue((result as UpdateCheckResult.Failure).message.contains("最新版本"))
    }

    // ========== performUpdate 测试 ==========

    /**
     * 测试：更新命令执行成功时应返回 Success
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `performUpdate should return success when update command succeeds`() = runBlocking {
        every { versionService.executeUpdateCommand() } returns "Update completed successfully"

        val result = versionService.performUpdate()

        assertTrue(result is UpdateResult.Success)
        assertEquals("Update completed successfully", (result as UpdateResult.Success).message)
    }

    /**
     * 测试：更新进程被 SIGKILL (exit code 137) 终止时应返回 Success
     *
     * 更新流程中 updater 替换二进制后会 kill 原进程，退出码 137 是正常行为。
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `performUpdate should return success when process is killed by SIGKILL during update`() = runBlocking {
        val sigkillOutput = "2026/03/12 15:53:16 port: 59025, install dir: /Users/user/PreCI\n" +
            "2026/03/12 15:53:19 [>]START stopping preci server...\n" +
            "2026/03/12 15:53:19 [+]SUCCESS shutdown server successfully\n" +
            "2026/03/12 15:53:19 [>]START 正在启动更新器: /Users/user/PreCI/preci_tmp/PreCI/preci-updater"
        every { versionService.executeUpdateCommand() } returns sigkillOutput

        val result = versionService.performUpdate()

        assertTrue(result is UpdateResult.Success)
        assertTrue((result as UpdateResult.Success).message.contains("shutdown server successfully"))
    }

    /**
     * 测试：更新命令执行失败时应返回 Failure
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `performUpdate should return failure when update command fails`() = runBlocking {
        every { versionService.executeUpdateCommand() } throws RuntimeException("preci update 命令执行失败（exitCode=1）: download error")

        val result = versionService.performUpdate()

        assertTrue(result is UpdateResult.Failure)
        assertTrue((result as UpdateResult.Failure).message.contains("更新失败"))
    }

    /**
     * 测试：更新命令超时时应返回 Failure
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `performUpdate should return failure when update command times out`() = runBlocking {
        every { versionService.executeUpdateCommand() } throws RuntimeException("preci update 命令执行超时（300s）")

        val result = versionService.performUpdate()

        assertTrue(result is UpdateResult.Failure)
        assertTrue((result as UpdateResult.Failure).message.contains("超时"))
    }

    // ========== normalizeVersion 测试 ==========

    /**
     * 测试：normalizeVersion 应正确去除 "v" 前缀
     */
    @Test
    fun `normalizeVersion should remove v prefix`() {
        assertEquals("1.0.0", versionService.normalizeVersion("v1.0.0"))
        assertEquals("1.0.0", versionService.normalizeVersion("V1.0.0"))
        assertEquals("1.0.0", versionService.normalizeVersion("1.0.0"))
    }

    /**
     * 测试：normalizeVersion 应 trim 空白字符
     */
    @Test
    fun `normalizeVersion should trim whitespace`() {
        assertEquals("1.0.0", versionService.normalizeVersion("  v1.0.0  "))
        assertEquals("1.0.0", versionService.normalizeVersion("\n1.0.0\n"))
    }

    /**
     * 测试：normalizeVersion 对空字符串应返回空字符串
     */
    @Test
    fun `normalizeVersion should handle empty string`() {
        assertEquals("", versionService.normalizeVersion(""))
        assertEquals("", versionService.normalizeVersion("  "))
    }

    /**
     * 测试：normalizeVersion 只去除第一个 "v" 前缀
     */
    @Test
    fun `normalizeVersion should only remove first v prefix`() {
        assertEquals("2.0.0-v1", versionService.normalizeVersion("v2.0.0-v1"))
    }

    // ========== 完整流程测试 ==========

    /**
     * 测试：完整的检查更新 -> 执行更新流程
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `full update flow should work when update is available`() = runBlocking {
        coEvery { versionService.getCurrentVersion() } returns VersionResult.Success("1.0.0")
        coEvery { versionService.getLatestVersion() } returns VersionResult.Success("2.0.0")
        every { versionService.executeUpdateCommand() } returns "Updated to 2.0.0"

        val checkResult = versionService.checkForUpdate()
        assertTrue(checkResult is UpdateCheckResult.UpdateAvailable)

        val updateResult = versionService.performUpdate()
        assertTrue(updateResult is UpdateResult.Success)
    }

    /**
     * 测试：版本号带空白字符时版本比较仍然正确
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `checkForUpdate should handle whitespace in version strings`() = runBlocking {
        coEvery { versionService.getCurrentVersion() } returns VersionResult.Success("  1.0.0  ")
        coEvery { versionService.getLatestVersion() } returns VersionResult.Success("1.0.0\n")

        val result = versionService.checkForUpdate()

        assertTrue(result is UpdateCheckResult.AlreadyLatest)
    }
}
