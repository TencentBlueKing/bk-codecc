package com.codecc.preci.service.auth

import com.codecc.preci.BaseTest
import com.codecc.preci.api.model.response.LoginResponse
import com.codecc.preci.api.model.response.ProjectInfo
import com.codecc.preci.api.model.response.ProjectListResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * AuthService 单元测试
 *
 * 测试鉴权服务的数据模型和基本功能。
 * 登录状态由 Local Server 管理，插件端不缓存。
 *
 * @since 1.0
 */
@DisplayName("AuthService 测试")
class AuthServiceTest : BaseTest() {

    @Test
    @DisplayName("测试 LoginResult 密封类 - Success")
    fun testLoginResultSuccess() {
        val response = LoginResponse(projectId = "testProject", userId = "testUser")
        val result = LoginResult.Success(response)

        assertTrue(result is LoginResult.Success)
        assertEquals("testProject", result.response.projectId)
        assertEquals("testUser", result.response.userId)
    }

    @Test
    @DisplayName("测试 LoginResult 密封类 - Failure")
    fun testLoginResultFailure() {
        val errorMessage = "登录失败"
        val cause = Exception("Network error")
        val result = LoginResult.Failure(errorMessage, cause)

        assertTrue(result is LoginResult.Failure)
        assertEquals(errorMessage, result.message)
        assertNotNull(result.cause)
        assertEquals("Network error", result.cause?.message)
    }

    @Test
    @DisplayName("测试 LoginResult.Failure 不带 cause")
    fun testLoginResultFailureWithoutCause() {
        val result = LoginResult.Failure("登录失败")

        assertTrue(result is LoginResult.Failure)
        assertEquals("登录失败", result.message)
        assertNull(result.cause)
    }

    @Test
    @DisplayName("测试 LoginResult 类型安全")
    fun testLoginResultTypeSafety() {
        val successResult: LoginResult = LoginResult.Success(
            LoginResponse("project", "user")
        )
        val failureResult: LoginResult = LoginResult.Failure("error")

        // 使用 when 表达式确保类型安全
        val successMessage = when (successResult) {
            is LoginResult.Success -> "Success: ${successResult.response.userId}"
            is LoginResult.Failure -> "Failure: ${successResult.message}"
        }
        assertTrue(successMessage.contains("Success"))

        val failureMessage = when (failureResult) {
            is LoginResult.Success -> "Success: ${failureResult.response.userId}"
            is LoginResult.Failure -> "Failure: ${failureResult.message}"
        }
        assertTrue(failureMessage.contains("Failure"))
    }

    @Test
    @DisplayName("测试 LoginResult.Success 数据类功能")
    fun testLoginResultSuccessDataClass() {
        val response1 = LoginResponse("project1", "user1")
        val response2 = LoginResponse("project1", "user1")
        val result1 = LoginResult.Success(response1)
        val result2 = LoginResult.Success(response2)

        // 测试相等性
        assertEquals(result1, result2)

        // 测试 toString
        val toString = result1.toString()
        assertTrue(toString.contains("Success"))
        assertTrue(toString.contains("user1"))
    }

    @Test
    @DisplayName("测试 LoginResult.Failure 数据类功能")
    fun testLoginResultFailureDataClass() {
        val cause = Exception("test")
        val result1 = LoginResult.Failure("error1", cause)
        val result2 = LoginResult.Failure("error1", cause)

        // 测试相等性
        assertEquals(result1, result2)

        // 测试 toString
        val toString = result1.toString()
        assertTrue(toString.contains("Failure"))
        assertTrue(toString.contains("error1"))
    }

    // ========== ProjectListResult 测试 ==========

    @Test
    @DisplayName("测试 ProjectListResult 密封类 - Success")
    fun testProjectListResultSuccess() {
        val projects = listOf(
            ProjectInfo("project1", "Test Project 1"),
            ProjectInfo("project2", "Test Project 2")
        )
        val response = ProjectListResponse(projects)
        val result = ProjectListResult.Success(projects, response)

        assertTrue(result is ProjectListResult.Success)
        assertEquals(2, result.projects.size)
        assertEquals("project1", result.projects[0].projectId)
        assertEquals("Test Project 1", result.projects[0].projectName)
    }

    @Test
    @DisplayName("测试 ProjectListResult 密封类 - Failure")
    fun testProjectListResultFailure() {
        val errorMessage = "获取项目列表失败"
        val cause = Exception("Network error")
        val result = ProjectListResult.Failure(errorMessage, cause)

        assertTrue(result is ProjectListResult.Failure)
        assertEquals(errorMessage, result.message)
        assertNotNull(result.cause)
        assertEquals("Network error", result.cause?.message)
    }

    @Test
    @DisplayName("测试 ProjectListResult 空列表")
    fun testProjectListResultEmptyList() {
        val projects = emptyList<ProjectInfo>()
        val response = ProjectListResponse(projects)
        val result = ProjectListResult.Success(projects, response)

        assertTrue(result is ProjectListResult.Success)
        assertTrue(result.projects.isEmpty())
    }

    @Test
    @DisplayName("测试 ProjectListResult 类型安全")
    fun testProjectListResultTypeSafety() {
        val successResult: ProjectListResult = ProjectListResult.Success(
            emptyList(),
            ProjectListResponse(emptyList())
        )
        val failureResult: ProjectListResult = ProjectListResult.Failure("error")

        // 使用 when 表达式确保类型安全
        val successMessage = when (successResult) {
            is ProjectListResult.Success -> "Success: ${successResult.projects.size} projects"
            is ProjectListResult.Failure -> "Failure: ${successResult.message}"
        }
        assertTrue(successMessage.contains("Success"))

        val failureMessage = when (failureResult) {
            is ProjectListResult.Success -> "Success: ${failureResult.projects.size} projects"
            is ProjectListResult.Failure -> "Failure: ${failureResult.message}"
        }
        assertTrue(failureMessage.contains("Failure"))
    }

    // ========== SetProjectResult 测试 ==========

    @Test
    @DisplayName("测试 SetProjectResult 密封类 - Success")
    fun testSetProjectResultSuccess() {
        val result = SetProjectResult.Success("project123")

        assertTrue(result is SetProjectResult.Success)
        assertEquals("project123", result.projectId)
    }

    @Test
    @DisplayName("测试 SetProjectResult 密封类 - Failure")
    fun testSetProjectResultFailure() {
        val errorMessage = "设置项目失败"
        val cause = Exception("Project not found")
        val result = SetProjectResult.Failure(errorMessage, cause)

        assertTrue(result is SetProjectResult.Failure)
        assertEquals(errorMessage, result.message)
        assertNotNull(result.cause)
        assertEquals("Project not found", result.cause?.message)
    }

    @Test
    @DisplayName("测试 SetProjectResult 类型安全")
    fun testSetProjectResultTypeSafety() {
        val successResult: SetProjectResult = SetProjectResult.Success("project123")
        val failureResult: SetProjectResult = SetProjectResult.Failure("error")

        // 使用 when 表达式确保类型安全
        val successMessage = when (successResult) {
            is SetProjectResult.Success -> "Success: ${successResult.projectId}"
            is SetProjectResult.Failure -> "Failure: ${successResult.message}"
        }
        assertTrue(successMessage.contains("Success"))

        val failureMessage = when (failureResult) {
            is SetProjectResult.Success -> "Success: ${failureResult.projectId}"
            is SetProjectResult.Failure -> "Failure: ${failureResult.message}"
        }
        assertTrue(failureMessage.contains("Failure"))
    }

    @Test
    @DisplayName("测试 SetProjectResult.Success 数据类功能")
    fun testSetProjectResultSuccessDataClass() {
        val result1 = SetProjectResult.Success("project123")
        val result2 = SetProjectResult.Success("project123")

        // 测试相等性
        assertEquals(result1, result2)

        // 测试 toString
        val toString = result1.toString()
        assertTrue(toString.contains("Success"))
        assertTrue(toString.contains("project123"))
    }

    // ========== ProjectInfo 测试 ==========

    @Test
    @DisplayName("测试 ProjectInfo 数据类")
    fun testProjectInfo() {
        val project = ProjectInfo("project001", "My Project")

        assertEquals("project001", project.projectId)
        assertEquals("My Project", project.projectName)
    }

    @Test
    @DisplayName("测试 ProjectInfo 相等性")
    fun testProjectInfoEquality() {
        val project1 = ProjectInfo("project001", "My Project")
        val project2 = ProjectInfo("project001", "My Project")
        val project3 = ProjectInfo("project002", "Other Project")

        assertEquals(project1, project2)
        assertNotEquals(project1, project3)
    }

    @Test
    @DisplayName("测试 ProjectListResponse 数据类")
    fun testProjectListResponse() {
        val projects = listOf(
            ProjectInfo("p1", "Project 1"),
            ProjectInfo("p2", "Project 2")
        )
        val response = ProjectListResponse(projects)

        assertEquals(2, response.projects.size)
        assertEquals("p1", response.projects[0].projectId)
        assertEquals("Project 1", response.projects[0].projectName)
    }

    // ========== GetCurrentProjectResult 测试 ==========

    @Test
    @DisplayName("测试 GetCurrentProjectResult 密封类 - Success")
    fun testGetCurrentProjectResultSuccess() {
        val result = GetCurrentProjectResult.Success("project123")

        assertTrue(result is GetCurrentProjectResult.Success)
        assertEquals("project123", result.projectId)
    }

    @Test
    @DisplayName("测试 GetCurrentProjectResult 密封类 - Success（空项目 ID）")
    fun testGetCurrentProjectResultSuccessEmpty() {
        val result = GetCurrentProjectResult.Success("")

        assertTrue(result is GetCurrentProjectResult.Success)
        assertEquals("", result.projectId)
    }

    @Test
    @DisplayName("测试 GetCurrentProjectResult 密封类 - Failure")
    fun testGetCurrentProjectResultFailure() {
        val errorMessage = "获取当前绑定项目失败"
        val cause = Exception("Network error")
        val result = GetCurrentProjectResult.Failure(errorMessage, cause)

        assertTrue(result is GetCurrentProjectResult.Failure)
        assertEquals(errorMessage, result.message)
        assertNotNull(result.cause)
        assertEquals("Network error", result.cause?.message)
    }

    @Test
    @DisplayName("测试 GetCurrentProjectResult.Failure 不带 cause")
    fun testGetCurrentProjectResultFailureWithoutCause() {
        val result = GetCurrentProjectResult.Failure("获取失败")

        assertTrue(result is GetCurrentProjectResult.Failure)
        assertEquals("获取失败", result.message)
        assertNull(result.cause)
    }

    @Test
    @DisplayName("测试 GetCurrentProjectResult 类型安全")
    fun testGetCurrentProjectResultTypeSafety() {
        val successResult: GetCurrentProjectResult = GetCurrentProjectResult.Success("project123")
        val failureResult: GetCurrentProjectResult = GetCurrentProjectResult.Failure("error")

        // 使用 when 表达式确保类型安全
        val successMessage = when (successResult) {
            is GetCurrentProjectResult.Success -> "Success: ${successResult.projectId}"
            is GetCurrentProjectResult.Failure -> "Failure: ${successResult.message}"
        }
        assertTrue(successMessage.contains("Success"))

        val failureMessage = when (failureResult) {
            is GetCurrentProjectResult.Success -> "Success: ${failureResult.projectId}"
            is GetCurrentProjectResult.Failure -> "Failure: ${failureResult.message}"
        }
        assertTrue(failureMessage.contains("Failure"))
    }

    @Test
    @DisplayName("测试 GetCurrentProjectResult.Success 数据类功能")
    fun testGetCurrentProjectResultSuccessDataClass() {
        val result1 = GetCurrentProjectResult.Success("project123")
        val result2 = GetCurrentProjectResult.Success("project123")

        // 测试相等性
        assertEquals(result1, result2)

        // 测试 toString
        val toString = result1.toString()
        assertTrue(toString.contains("Success"))
        assertTrue(toString.contains("project123"))
    }

    /*
     * 以下是集成测试，需要 PreCI Local Server 运行和 IntelliJ 平台环境
     * 手动测试时可以取消注释
     *
    @Test
    @DisplayName("测试 OAuth 登录流程")
    fun testLoginWithOAuth() = runBlocking {
        val authService = AuthService.getInstance()

        val result = authService.loginWithOAuth()

        when (result) {
            is LoginResult.Success -> {
                println("OAuth 登录成功: ${result.response.userId}")
            }
            is LoginResult.Failure -> {
                println("OAuth 登录失败: ${result.message}")
            }
        }
    }

    @Test
    @DisplayName("测试获取项目列表")
    fun testGetProjects() = runBlocking {
        val authService = AuthService.getInstance()

        val result = authService.getProjects()

        when (result) {
            is ProjectListResult.Success -> {
                println("获取项目列表成功: ${result.projects.size} 个项目")
                result.projects.forEach { p ->
                    println("  - ${p.projectId}: ${p.projectName}")
                }
            }
            is ProjectListResult.Failure -> {
                println("获取项目列表失败: ${result.message}")
            }
        }
    }

    @Test
    @DisplayName("测试设置项目")
    fun testSetProject() = runBlocking {
        val authService = AuthService.getInstance()

        val result = authService.setProject("test_project_001")

        when (result) {
            is SetProjectResult.Success -> {
                println("设置项目成功: ${result.projectId}")
            }
            is SetProjectResult.Failure -> {
                println("设置项目失败: ${result.message}")
            }
        }
    }
    */
}

