package com.codecc.preci.service.auth

import com.codecc.preci.api.model.response.LoginResponse
import com.codecc.preci.api.model.response.ProjectInfo
import com.codecc.preci.api.model.response.ProjectListResponse

/**
 * 登录结果密封类
 *
 * 表示登录操作的结果，使用密封类保证类型安全。
 *
 * @since 1.0
 */
sealed class LoginResult {
    /**
     * 登录成功
     *
     * @property response 登录响应，包含用户 ID 和项目 ID
     *
     * @since 1.0
     */
    data class Success(val response: LoginResponse) : LoginResult()

    /**
     * 登录失败
     *
     * @property message 失败原因描述
     * @property cause 原始异常（如果有）
     *
     * @since 1.0
     */
    data class Failure(val message: String, val cause: Throwable? = null) : LoginResult()
}

/**
 * 获取项目列表结果密封类
 *
 * 表示获取项目列表操作的结果，使用密封类保证类型安全。
 *
 * @since 1.0
 */
sealed class ProjectListResult {
    /**
     * 获取成功
     *
     * @property projects 项目列表
     * @property response 原始 API 响应
     *
     * @since 1.0
     */
    data class Success(
        val projects: List<ProjectInfo>,
        val response: ProjectListResponse
    ) : ProjectListResult()

    /**
     * 获取失败
     *
     * @property message 失败原因描述
     * @property cause 原始异常（如果有）
     *
     * @since 1.0
     */
    data class Failure(val message: String, val cause: Throwable? = null) : ProjectListResult()
}

/**
 * 设置项目结果密封类
 *
 * 表示设置项目操作的结果，使用密封类保证类型安全。
 *
 * @since 1.0
 */
sealed class SetProjectResult {
    /**
     * 设置成功
     *
     * @property projectId 设置的项目 ID
     *
     * @since 1.0
     */
    data class Success(val projectId: String) : SetProjectResult()

    /**
     * 设置失败
     *
     * @property message 失败原因描述
     * @property cause 原始异常（如果有）
     *
     * @since 1.0
     */
    data class Failure(val message: String, val cause: Throwable? = null) : SetProjectResult()
}

/**
 * 获取当前绑定项目结果密封类
 *
 * 表示获取当前绑定项目操作的结果，使用密封类保证类型安全。
 *
 * @since 1.0
 */
sealed class GetCurrentProjectResult {
    /**
     * 获取成功
     *
     * @property projectId 当前绑定的项目 ID，如果未绑定则为空字符串
     *
     * @since 1.0
     */
    data class Success(val projectId: String) : GetCurrentProjectResult()

    /**
     * 获取失败
     *
     * @property message 失败原因描述
     * @property cause 原始异常（如果有）
     *
     * @since 1.0
     */
    data class Failure(val message: String, val cause: Throwable? = null) : GetCurrentProjectResult()
}

