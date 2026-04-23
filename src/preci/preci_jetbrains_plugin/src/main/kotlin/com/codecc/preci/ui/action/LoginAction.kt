package com.codecc.preci.ui.action

import com.codecc.preci.service.auth.AuthService
import com.codecc.preci.service.auth.LoginResult
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import kotlinx.coroutines.runBlocking

/**
 * 登录 Action
 *
 * 触发 OAuth 浏览器登录流程，引导用户完成 BKAuth 认证。
 * 继承自 [AnAction]，可以通过菜单、工具栏或快捷键触发。
 *
 * **功能流程：**
 * 1. 发起 OAuth Authorization Code + PKCE 流程
 * 2. 打开浏览器跳转到 BKAuth 授权页
 * 3. 用户在浏览器中完成授权
 * 4. 回调将 authorization code 传回插件
 * 5. 用 code 换 token，并交给 Local Server 管理
 *
 * **使用场景：**
 * - 用户首次使用插件
 * - 认证过期需要重新登录
 * - 用户主动点击"登录"菜单项
 *
 * **注册方式：**
 * 在 plugin.xml 中注册为 Action：
 * ```xml
 * <action
 *     id="PreCI.Login"
 *     class="com.codecc.preci.ui.action.LoginAction"
 *     text="Login to PreCI"
 *     description="Login to PreCI via OAuth"
 *     icon="AllIcons.Actions.Login">
 * </action>
 * ```
 *
 * @since 2.0
 */
class LoginAction : AnAction() {

    /**
     * 执行 Action
     *
     * 当用户触发该 Action 时调用。
     * 发起 OAuth 浏览器登录流程。
     *
     * @param event Action 事件，包含上下文信息（项目、编辑器等）
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        object : Task.Backgroundable(project, "登录到 PreCI", true) {
            private var loginResult: LoginResult? = null

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "正在执行 OAuth 登录，请在浏览器中完成认证..."

                val authService = AuthService.getInstance()
                loginResult = runBlocking {
                    authService.loginWithOAuth()
                }
            }

            override fun onSuccess() {
                when (loginResult) {
                    is LoginResult.Success -> { /* AuthService 已显示通知 */ }
                    is LoginResult.Failure -> { /* AuthService 已显示通知 */ }
                    null -> {
                        showNotification(
                            "登录失败",
                            "登录过程中发生未知错误",
                            NotificationType.ERROR
                        )
                    }
                }
            }

            override fun onThrowable(error: Throwable) {
                showNotification(
                    "登录失败",
                    "登录过程中发生异常：${error.message}",
                    NotificationType.ERROR
                )
            }

            override fun onCancel() {
                showNotification(
                    "登录取消",
                    "用户取消了登录操作",
                    NotificationType.WARNING
                )
            }

            private fun showNotification(title: String, content: String, type: NotificationType) {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("PreCI.Auth")
                    .createNotification(title, content, type)
                    .notify(project)
            }
        }.queue()
    }

    /**
     * 更新 Action 状态
     *
     * 决定 Action 是否可用和可见。
     * 只有在项目存在时才启用该 Action。
     *
     * @param event Action 事件
     */
    override fun update(event: AnActionEvent) {
        // 只有在项目存在时才启用
        event.presentation.isEnabledAndVisible = event.project != null
    }
}

