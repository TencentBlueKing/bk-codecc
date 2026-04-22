/**
 * 鉴权服务模块
 *
 * 提供 OAuth (BKAuth) 登录和蓝盾项目管理功能，负责与 PreCI Local Server 通信。
 * 登录状态和认证信息由 Local Server 管理（有效期 180 天），插件端不缓存。
 *
 * **核心类：**
 * - [AuthService] - 鉴权服务接口
 * - [AuthServiceImpl] - 鉴权服务实现
 * - [LoginResult] - 登录结果密封类
 *
 * **使用方式：**
 * ```kotlin
 * val authService = AuthService.getInstance()
 *
 * val result = authService.loginWithOAuth()
 * when (result) {
 *     is LoginResult.Success -> println("登录成功")
 *     is LoginResult.Failure -> println("登录失败: ${result.message}")
 * }
 * ```
 *
 * @since 2.0
 */
package com.codecc.preci.service.auth
