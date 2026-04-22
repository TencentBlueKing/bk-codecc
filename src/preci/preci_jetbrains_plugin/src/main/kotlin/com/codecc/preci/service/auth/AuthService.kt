package com.codecc.preci.service.auth

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

/**
 * 鉴权服务接口
 *
 * 负责通过 OAuth (BKAuth) 完成用户身份认证，并管理蓝盾项目绑定。
 * 登录状态和认证信息由 Local Server 管理（有效期 180 天），插件端不缓存。
 *
 * **登录方式：**
 *
 * - **OAuth 浏览器登录**：通过 BKAuth Authorization Code + PKCE 流程，
 *   在浏览器中完成用户授权，获取 token 后交给 Local Server 管理。
 *
 * **使用场景：**
 * - 用户首次使用插件时
 * - API 调用返回 401 认证失败时
 * - 用户主动点击"重新登录"时
 *
 * **使用示例：**
 * ```kotlin
 * val authService = AuthService.getInstance()
 *
 * val result = authService.loginWithOAuth()
 * when (result) {
 *     is LoginResult.Success -> println("登录成功: ${result.response.userId}")
 *     is LoginResult.Failure -> println("登录失败: ${result.message}")
 * }
 * ```
 *
 * **线程安全性：**
 * - 所有公共方法都是挂起函数，支持协程
 * - 可以在任意协程作用域中调用
 *
 * @since 2.0
 */
@Service(Service.Level.APP)
interface AuthService {

    /**
     * 通过 OAuth 浏览器流程登录
     *
     * 发起 OAuth Authorization Code + PKCE 流程：
     * 1. 打开浏览器跳转到 BKAuth 授权页
     * 2. 用户在浏览器完成授权
     * 3. 回调将 authorization code 传回插件
     * 4. 用 code 换 token
     * 5. 将 token 交给 Local Server 管理
     *
     * @return 登录结果
     *
     * @since 2.0
     */
    suspend fun loginWithOAuth(): LoginResult

    /**
     * 获取项目列表
     *
     * 调用 PreCI Local Server 的 `GET /auth/list/projects` 接口获取当前用户有权限的蓝盾项目列表。
     *
     * **查询流程：**
     * 1. 调用 `/auth/list/projects` 接口
     * 2. Local Server 返回当前用户有权限的项目列表
     * 3. 每个项目包含：ID、名称
     * 4. 返回查询结果
     *
     * **返回数据说明：**
     * - `projectId`：项目唯一标识 ID
     * - `projectName`：项目显示名称
     *
     * **注意事项：**
     * - 需要先登录才能获取项目列表
     * - Local Server 需要处于运行状态
     *
     * @return 项目列表查询结果，包含成功或失败信息
     *
     * @since 1.0
     */
    suspend fun getProjects(): ProjectListResult

    /**
     * 设置当前项目
     *
     * 调用 PreCI Local Server 的 `GET /auth/project/{projectId}` 接口设置当前使用的蓝盾项目。
     *
     * **设置流程：**
     * 1. 调用 `/auth/project/{projectId}` 接口
     * 2. Local Server 将项目设置为当前项目
     * 3. 后续的扫描操作将使用此项目配置
     * 4. 返回设置结果
     *
     * **注意事项：**
     * - 需要先登录才能设置项目
     * - projectId 必须是用户有权限的项目
     * - Local Server 需要处于运行状态
     *
     * @param projectId 要设置的项目 ID
     * @return 设置结果，包含成功或失败信息
     *
     * @since 1.0
     */
    suspend fun setProject(projectId: String): SetProjectResult

    /**
     * 获取当前绑定的项目
     *
     * 调用 PreCI Local Server 的 `GET /auth/project` 接口获取当前绑定的蓝盾项目 ID。
     *
     * **查询流程：**
     * 1. 调用 `/auth/project` 接口
     * 2. Local Server 返回当前绑定的项目 ID
     * 3. 如果未绑定项目，返回空字符串
     * 4. 返回查询结果
     *
     * **注意事项：**
     * - 需要先登录才能获取绑定项目
     * - Local Server 需要处于运行状态
     *
     * @return 获取结果，包含项目 ID 或失败信息
     *
     * @since 1.0
     */
    suspend fun getCurrentProject(): GetCurrentProjectResult

    companion object {
        /**
         * 获取 AuthService 实例
         *
         * 从 IntelliJ Platform 的服务容器中获取 Application 级的 AuthService 实例。
         *
         * @return AuthService 实例
         *
         * @since 1.0
         */
        @JvmStatic
        fun getInstance(): AuthService {
            return ApplicationManager.getApplication().getService(AuthService::class.java)
        }
    }
}

