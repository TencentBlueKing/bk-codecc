package com.codecc.preci.service.auth

import com.codecc.preci.api.client.PreCIApiClient
import com.codecc.preci.core.http.ApiErrorHandler
import com.codecc.preci.core.http.BusinessException
import com.codecc.preci.core.http.NetworkException
import com.codecc.preci.core.http.PreCIApiException
import com.codecc.preci.core.http.ServerBusyException
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.oauth.BKAuthClient
import com.codecc.preci.service.oauth.OAuthConfigLoader
import com.codecc.preci.service.oauth.OAuthException
import com.codecc.preci.service.oauth.OAuthService
import com.codecc.preci.service.oauth.PKCEHelper
import com.codecc.preci.service.server.ServerStartupActivity
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 鉴权服务实现
 *
 * 实现 [AuthService] 接口，通过 OAuth (BKAuth) 完成用户身份认证。
 * 登录状态和认证信息由 Local Server 管理，插件端不缓存。
 *
 * **实现特性：**
 * - 协程支持：所有 I/O 操作在 Dispatchers.IO 中执行
 * - OAuth PKCE 流程：通过浏览器完成 BKAuth 授权
 * - 详细日志：记录所有登录操作
 * - 用户友好：提供清晰的错误信息和 IDE 通知
 *
 * **OAuth 认证流程：**
 * 1. 创建 OAuth 会话，生成 PKCE code_verifier / code_challenge
 * 2. 打开浏览器跳转到 BKAuth 授权页
 * 3. 用户授权后，回调将 authorization code 传回插件
 * 4. 用 code + code_verifier 换 token
 * 5. 将 token 交给 Local Server 管理
 *
 * @since 2.0
 */
class AuthServiceImpl : AuthService {

    private val logger = PreCILogger.getLogger(AuthServiceImpl::class.java)
    private val apiClient = PreCIApiClient()

    override suspend fun loginWithOAuth(): LoginResult = withContext(Dispatchers.IO) {
        try {
            logger.info("Starting OAuth login flow")

            val oauthService = com.intellij.openapi.application.ApplicationManager.getApplication()
                .getService(OAuthService::class.java)
            val config = OAuthConfigLoader.load()

            val state = oauthService.createSession()
            val codeVerifier = oauthService.getSessionCodeVerifier(state)
                ?: throw OAuthException("Failed to get code verifier for session")
            val codeChallenge = PKCEHelper.generateCodeChallenge(codeVerifier)

            val idePort = org.jetbrains.ide.BuiltInServerManager.getInstance().port
            val redirectUri = "http://127.0.0.1:$idePort${config.redirectPath}"

            val authorizeUrl = BKAuthClient.buildAuthorizeUrl(config, codeChallenge, state, redirectUri)
            logger.info("Opening browser for OAuth authorization")
            com.intellij.ide.BrowserUtil.browse(authorizeUrl)

            val code = oauthService.awaitAuthorizationCode(state, OAUTH_FLOW_TIMEOUT_MS)
            logger.info("Received authorization code, exchanging for token")

            val tokenResponse = BKAuthClient.exchangeCodeForToken(
                config, code, codeVerifier, redirectUri
            )
            logger.info("Token exchange successful, passing to Local Server")

            val deviceLoginRequest = com.codecc.preci.api.model.request.OAuthDeviceLoginRequest(
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken,
                expiresIn = tokenResponse.expiresIn
            )
            val loginResponse = apiClient.oauthDeviceLogin(deviceLoginRequest)
            logger.info("OAuth login completed: userId=${loginResponse.userId}")

            showNotification(
                "登录成功",
                "用户 ${loginResponse.userId} 已成功登录",
                NotificationType.INFORMATION
            )

            LoginResult.Success(loginResponse)

        } catch (e: OAuthException) {
            logger.error("OAuth login failed: ${e.message}", e)
            val message = "OAuth 授权失败：${e.message}"
            showNotification("登录失败", message, NotificationType.ERROR)
            LoginResult.Failure(message, e)

        } catch (e: BusinessException) {
            logger.error("OAuth login business error: ${e.message}", e)
            val message = "登录失败：${e.message}"
            showNotification("登录失败", message, NotificationType.ERROR)
            LoginResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("OAuth login network error: ${e.message}", e)
            val message = formatNetworkError(e)
            showNotification("登录失败", message, NotificationType.ERROR)
            LoginResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("OAuth login unexpected error: ${e.message}", e)
            val message = "未知错误：${e.message}"
            showNotification("登录失败", message, NotificationType.ERROR)
            LoginResult.Failure(message, e)
        }
    }

    private fun formatNetworkError(e: NetworkException): String {
        return if (e is ServerBusyException) {
            "服务响应超时：${e.message.orEmpty()}"
        } else {
            "网络错误：${e.message.orEmpty()}\n请检查 PreCI Local Server 是否正常运行"
        }
    }

    private fun showNotification(title: String, content: String, type: NotificationType) {
        try {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("PreCI.Auth")
                .createNotification(title, content, type)
                .notify(null)
        } catch (e: Exception) {
            logger.error("显示通知失败: ${e.message}", e)
        }
    }

    override suspend fun getProjects(): ProjectListResult = withContext(Dispatchers.IO) {
        try {
            logger.info("开始获取项目列表")
            val response = apiClient.getProjects()
            logger.info("成功获取项目列表，共 ${response.projects.size} 个项目")

            // 记录项目详情
            if (response.projects.isNotEmpty()) {
                response.projects.forEach { project ->
                    logger.debug("项目: ${project.projectId} - ${project.projectName}")
                }
            } else {
                logger.warn("项目列表为空")
            }

            ProjectListResult.Success(response.projects, response)

        } catch (e: BusinessException) {
            logger.error("获取项目列表失败 (业务异常): ${e.message}", e)

            // 尝试自动处理错误（如自动登录、提示绑定项目）
            val handled = ApiErrorHandler.handle(e, null)

            // 如果错误已被处理（如项目 ID 无效），直接返回失败结果，不再显示额外的错误提示
            if (handled) {
                val message = e.message.orEmpty()
                return@withContext ProjectListResult.Failure(message, e)
            }

            val message = when (e.httpCode) {
                401 -> "认证失败：请先登录后再试"
                403 -> "权限不足：无权限获取项目列表"
                else -> "获取项目列表失败：${e.message}"
            }
            showNotification("获取项目列表失败", message, NotificationType.ERROR)
            ProjectListResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("获取项目列表网络错误: ${e.message}", e)
            val message = formatNetworkError(e)
            if (ServerStartupActivity.serverReady.isCompleted) {
                showNotification("获取项目列表失败", message, NotificationType.ERROR)
            } else {
                logger.info("服务尚在启动中，抑制错误通知")
            }
            ProjectListResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("获取项目列表 API 错误: ${e.message}", e)
            val message = "服务错误：${e.message}"
            showNotification("获取项目列表失败", message, NotificationType.ERROR)
            ProjectListResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("获取项目列表异常: ${e.message}", e)
            val message = "未知错误：${e.message}"
            showNotification("获取项目列表失败", message, NotificationType.ERROR)
            ProjectListResult.Failure(message, e)
        }
    }

    override suspend fun setProject(projectId: String): SetProjectResult = withContext(Dispatchers.IO) {
        try {
            logger.info("开始设置项目: $projectId")
            apiClient.setProject(projectId)
            logger.info("成功设置项目: $projectId")

            showNotification(
                "设置项目成功",
                "已将当前项目设置为: $projectId",
                NotificationType.INFORMATION
            )

            SetProjectResult.Success(projectId)

        } catch (e: BusinessException) {
            logger.error("设置项目失败 (业务异常): ${e.message}", e)

            // 尝试自动处理错误（如自动登录、提示绑定项目）
            val handled = ApiErrorHandler.handle(e, null)

            // 如果错误已被处理（如项目 ID 无效），直接返回失败结果，不再显示额外的错误提示
            if (handled) {
                val message = e.message.orEmpty()
                return@withContext SetProjectResult.Failure(message, e)
            }

            val message = when (e.httpCode) {
                401 -> "认证失败：请先登录后再试"
                403 -> "权限不足：无权限访问该项目"
                404 -> "项目不存在：项目 ID $projectId 不存在"
                else -> "设置项目失败：${e.message}"
            }
            showNotification("设置项目失败", message, NotificationType.ERROR)
            SetProjectResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("设置项目网络错误: ${e.message}", e)
            val message = formatNetworkError(e)
            showNotification("设置项目失败", message, NotificationType.ERROR)
            SetProjectResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("设置项目 API 错误: ${e.message}", e)
            val message = "服务错误：${e.message}"
            showNotification("设置项目失败", message, NotificationType.ERROR)
            SetProjectResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("设置项目异常: ${e.message}", e)
            val message = "未知错误：${e.message}"
            showNotification("设置项目失败", message, NotificationType.ERROR)
            SetProjectResult.Failure(message, e)
        }
    }

    override suspend fun getCurrentProject(): GetCurrentProjectResult = withContext(Dispatchers.IO) {
        try {
            logger.info("开始获取当前绑定项目")
            val response = apiClient.getCurrentProject()
            logger.info("成功获取当前绑定项目: ${response.projectId}")

            GetCurrentProjectResult.Success(response.projectId)

        } catch (e: BusinessException) {
            logger.error("获取当前绑定项目失败 (业务异常): ${e.message}", e)

            // 尝试自动处理错误（如自动登录、提示绑定项目）
            val handled = ApiErrorHandler.handle(e, null)

            // 如果错误已被处理（如项目 ID 无效），直接返回失败结果，不再显示额外的错误提示
            if (handled) {
                val message = e.message.orEmpty()
                return@withContext GetCurrentProjectResult.Failure(message, e)
            }

            val message = when (e.httpCode) {
                401 -> "认证失败：请先登录后再试"
                else -> "获取当前绑定项目失败：${e.message}"
            }
            GetCurrentProjectResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("获取当前绑定项目网络错误: ${e.message}", e)
            val message = formatNetworkError(e)
            GetCurrentProjectResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("获取当前绑定项目 API 错误: ${e.message}", e)
            val message = "服务错误：${e.message}"
            GetCurrentProjectResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("获取当前绑定项目异常: ${e.message}", e)
            val message = "未知错误：${e.message}"
            GetCurrentProjectResult.Failure(message, e)
        }
    }

    companion object {
        const val OAUTH_FLOW_TIMEOUT_MS = 120_000L
    }
}

