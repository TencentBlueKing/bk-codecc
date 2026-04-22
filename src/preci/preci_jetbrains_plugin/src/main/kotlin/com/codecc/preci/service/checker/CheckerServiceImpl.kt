package com.codecc.preci.service.checker

import com.codecc.preci.api.client.PreCIApiClient
import com.codecc.preci.api.model.request.CheckerSetSelectRequest
import com.codecc.preci.core.config.PreCISettings
import com.codecc.preci.core.http.ApiErrorHandler
import com.codecc.preci.core.http.BusinessException
import com.codecc.preci.core.http.NetworkException
import com.codecc.preci.core.http.PreCIApiException
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.server.ServerStartupActivity
import com.codecc.preci.util.PathHelper
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * 规则集服务实现
 *
 * 实现 [CheckerService] 接口，提供规则集管理功能。
 * 包括规则集列表查询、规则集选择和配置持久化。
 *
 * **实现特性：**
 * - 协程支持：所有 I/O 操作在 Dispatchers.IO 中执行
 * - 配置持久化：通过 [PreCISettings] 持久化用户的规则集选择
 * - 详细日志：记录所有规则集操作
 * - 用户友好：提供清晰的错误信息和 IDE 通知
 *
 * **配置持久化机制：**
 * - 使用 IntelliJ Platform 的 `PersistentStateComponent` 机制
 * - 规则集选择保存在 IDE 的配置目录中（preci-settings.xml）
 * - 支持"记住规则集选择"功能，可以在 IDE 重启后自动应用
 *
 * @property project 当前项目
 *
 * @since 1.0
 */
class CheckerServiceImpl(private val project: Project) : CheckerService {

    private val logger = PreCILogger.getLogger(CheckerServiceImpl::class.java)
    private val apiClient = PreCIApiClient()

    override suspend fun getCheckerSetList(): CheckerSetListResult = withContext(Dispatchers.IO) {
        getCheckerSetListBlocking()
    }

    override fun getCheckerSetListBlocking(): CheckerSetListResult {
        return try {
            logger.info("开始获取规则集列表")

            val response = runBlocking { apiClient.getCheckerSetList() }

            // 将 API 响应转换为服务层模型
            val checkerSets = response.checkerSets.map { CheckerSetInfo.fromApiResponse(it) }

            logger.info("成功获取规则集列表，共 ${checkerSets.size} 个规则集")

            // 记录规则集详情
            if (checkerSets.isNotEmpty()) {
                checkerSets.forEach { set ->
                    logger.debug("规则集: ${set.id} - ${set.name} (${set.toolName})")
                }
            } else {
                logger.warn("规则集列表为空")
            }

            CheckerSetListResult.Success(checkerSets, response)

        } catch (e: BusinessException) {
            logger.error("获取规则集列表失败 (业务异常): ${e.message}", e)

            // 尝试自动处理错误（如自动登录、提示绑定项目）
            val handled = ApiErrorHandler.handle(e, project)

            // 如果错误已被处理（如项目 ID 无效），直接返回失败结果，不再显示额外的错误提示
            if (handled) {
                val message = e.message.orEmpty()
                return CheckerSetListResult.Failure(message, e)
            }

            val message = when (e.httpCode) {
                401 -> "认证失败：请先登录后再试"
                403 -> "权限不足：无权限获取规则集列表"
                404 -> "服务未就绪：Local Server 未初始化"
                else -> "获取规则集列表失败：${e.message}"
            }
            showNotification("获取规则集失败", message, NotificationType.ERROR)
            CheckerSetListResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("获取规则集列表失败 (网络异常): ${e.message}", e)
            val message = "网络错误：${e.message}\n请检查 PreCI Local Server 是否正常运行"
            if (ServerStartupActivity.serverReady.isCompleted) {
                showNotification("获取规则集失败", message, NotificationType.ERROR)
            } else {
                logger.info("服务尚在启动中，抑制错误通知")
            }
            CheckerSetListResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("获取规则集列表失败 (API 异常): ${e.message}", e)
            val message = "服务错误：${e.message}"
            showNotification("获取规则集失败", message, NotificationType.ERROR)
            CheckerSetListResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("获取规则集列表失败 (未知异常): ${e.message}", e)
            val message = "未知错误：${e.message}"
            showNotification("获取规则集失败", message, NotificationType.ERROR)
            CheckerSetListResult.Failure(message, e)
        }
    }

    override suspend fun selectCheckerSets(
        checkerSetIds: List<String>,
        projectRootDir: String?
    ): CheckerSetSelectResult = withContext(Dispatchers.IO) {
        selectCheckerSetsBlocking(checkerSetIds, projectRootDir)
    }

    override fun selectCheckerSetsBlocking(
        checkerSetIds: List<String>,
        projectRootDir: String?
    ): CheckerSetSelectResult {
        return try {
            val rootDir = (projectRootDir ?: project.basePath)?.let { PathHelper.toNativePath(it) }

            logger.info("开始选择规则集，项目根目录: $rootDir, 规则集: $checkerSetIds")

            if (checkerSetIds.isEmpty()) {
                logger.warn("规则集列表为空，将清除已选择的规则集（使用默认规则集）")
            }

            val request = CheckerSetSelectRequest(
                projectRootDir = rootDir,
                checkerSets = checkerSetIds
            )

            val response = runBlocking { apiClient.selectCheckerSet(request) }

            logger.info("成功选择规则集，项目: ${response.projectRoot}, 已选择: ${response.checkerSets}")

            // 如果启用了"记住规则集选择"，保存到本地配置
            if (isRememberCheckerSetsEnabled()) {
                saveSelectedCheckerSets(response.checkerSets)
            }

            // 显示成功通知
            val message = if (response.checkerSets.isEmpty()) {
                "已清除规则集选择，将使用默认规则集"
            } else {
                "成功选择 ${response.checkerSets.size} 个规则集"
            }
            showNotification("规则集选择成功", message, NotificationType.INFORMATION)

            CheckerSetSelectResult.Success(
                projectRoot = response.projectRoot,
                selectedSets = response.checkerSets,
                response = response
            )

        } catch (e: BusinessException) {
            logger.error("选择规则集失败 (业务异常): ${e.message}", e)

            // 尝试自动处理错误（如自动登录、提示绑定项目）
            val handled = ApiErrorHandler.handle(e, project)

            // 如果错误已被处理（如项目 ID 无效），直接返回失败结果，不再显示额外的错误提示
            if (handled) {
                val message = e.message.orEmpty()
                return CheckerSetSelectResult.Failure(message, e)
            }

            val message = when (e.httpCode) {
                401 -> "认证失败：请先登录后再试"
                403 -> "权限不足：无权限选择规则集"
                404 -> "规则集不存在：请检查规则集 ID 是否正确"
                else -> "选择规则集失败：${e.message}"
            }
            showNotification("选择规则集失败", message, NotificationType.ERROR)
            CheckerSetSelectResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("选择规则集失败 (网络异常): ${e.message}", e)
            val message = "网络错误：${e.message}\n请检查 PreCI Local Server 是否正常运行"
            showNotification("选择规则集失败", message, NotificationType.ERROR)
            CheckerSetSelectResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("选择规则集失败 (API 异常): ${e.message}", e)
            val message = "服务错误：${e.message}"
            showNotification("选择规则集失败", message, NotificationType.ERROR)
            CheckerSetSelectResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("选择规则集失败 (未知异常): ${e.message}", e)
            val message = "未知错误：${e.message}"
            showNotification("选择规则集失败", message, NotificationType.ERROR)
            CheckerSetSelectResult.Failure(message, e)
        }
    }

    override suspend fun unselectCheckerSets(
        checkerSetIds: List<String>,
        projectRootDir: String?
    ): CheckerSetSelectResult = withContext(Dispatchers.IO) {
        unselectCheckerSetsBlocking(checkerSetIds, projectRootDir)
    }

    override fun unselectCheckerSetsBlocking(
        checkerSetIds: List<String>,
        projectRootDir: String?
    ): CheckerSetSelectResult {
        return try {
            val rootDir = (projectRootDir ?: project.basePath)?.let { PathHelper.toNativePath(it) }

            logger.info("开始取消选择规则集，项目根目录: $rootDir, 规则集: $checkerSetIds")

            if (checkerSetIds.isEmpty()) {
                logger.info("取消选择的规则集列表为空，跳过")
                return CheckerSetSelectResult.Success(
                    projectRoot = rootDir ?: "",
                    selectedSets = emptyList(),
                    response = null
                )
            }

            val request = CheckerSetSelectRequest(
                projectRootDir = rootDir,
                checkerSets = checkerSetIds
            )

            val response = runBlocking { apiClient.unselectCheckerSet(request) }

            logger.info("成功取消选择规则集，项目: ${response.projectRoot}, 剩余已选择: ${response.checkerSets}")

            if (isRememberCheckerSetsEnabled()) {
                saveSelectedCheckerSets(response.checkerSets)
            }

            CheckerSetSelectResult.Success(
                projectRoot = response.projectRoot,
                selectedSets = response.checkerSets,
                response = response
            )

        } catch (e: BusinessException) {
            logger.error("取消选择规则集失败 (业务异常): ${e.message}", e)

            val handled = ApiErrorHandler.handle(e, project)
            if (handled) {
                val message = e.message.orEmpty()
                return CheckerSetSelectResult.Failure(message, e)
            }

            val message = when (e.httpCode) {
                401 -> "认证失败：请先登录后再试"
                403 -> "权限不足：无权限取消选择规则集"
                404 -> "规则集不存在：请检查规则集 ID 是否正确"
                else -> "取消选择规则集失败：${e.message}"
            }
            showNotification("取消选择规则集失败", message, NotificationType.ERROR)
            CheckerSetSelectResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("取消选择规则集失败 (网络异常): ${e.message}", e)
            val message = "网络错误：${e.message}\n请检查 PreCI Local Server 是否正常运行"
            showNotification("取消选择规则集失败", message, NotificationType.ERROR)
            CheckerSetSelectResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("取消选择规则集失败 (API 异常): ${e.message}", e)
            val message = "服务错误：${e.message}"
            showNotification("取消选择规则集失败", message, NotificationType.ERROR)
            CheckerSetSelectResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("取消选择规则集失败 (未知异常): ${e.message}", e)
            val message = "未知错误：${e.message}"
            showNotification("取消选择规则集失败", message, NotificationType.ERROR)
            CheckerSetSelectResult.Failure(message, e)
        }
    }

    override fun saveSelectedCheckerSets(checkerSetIds: List<String>): CheckerSetPersistResult {
        return try {
            logger.info("保存规则集选择到本地配置: $checkerSetIds")

            val settings = getSettings()
            if (settings != null) {
                // 清除现有选择
                settings.clearCheckerSets()
                // 添加新选择
                checkerSetIds.forEach { settings.addCheckerSet(it) }

                logger.info("规则集选择保存成功: ${settings.selectedCheckerSets}")
                CheckerSetPersistResult.Success(checkerSetIds)
            } else {
                val message = "无法获取配置服务"
                logger.error(message)
                CheckerSetPersistResult.Failure(message)
            }

        } catch (e: Exception) {
            val message = "保存规则集选择失败: ${e.message}"
            logger.error(message, e)
            CheckerSetPersistResult.Failure(message, e)
        }
    }

    override fun loadSelectedCheckerSets(): List<String> {
        return try {
            val settings = getSettings()
            val savedSets = settings?.selectedCheckerSets?.toList() ?: emptyList()

            logger.info("从本地配置加载规则集选择: $savedSets")
            savedSets

        } catch (e: Exception) {
            logger.error("加载规则集选择失败: ${e.message}", e)
            emptyList()
        }
    }

    override fun clearSelectedCheckerSets(): CheckerSetPersistResult {
        return try {
            logger.info("清除本地规则集选择配置")

            val settings = getSettings()
            if (settings != null) {
                settings.clearCheckerSets()
                logger.info("规则集选择配置已清除")
                CheckerSetPersistResult.Success(emptyList())
            } else {
                val message = "无法获取配置服务"
                logger.error(message)
                CheckerSetPersistResult.Failure(message)
            }

        } catch (e: Exception) {
            val message = "清除规则集选择配置失败: ${e.message}"
            logger.error(message, e)
            CheckerSetPersistResult.Failure(message, e)
        }
    }

    override suspend fun syncCheckerSetsToServer(): CheckerSetSelectResult? {
        // 检查是否启用了"记住规则集选择"功能
        if (!isRememberCheckerSetsEnabled()) {
            logger.info("未启用'记住规则集选择'功能，跳过同步")
            return null
        }

        // 加载本地保存的配置
        val savedSets = loadSelectedCheckerSets()
        if (savedSets.isEmpty()) {
            logger.info("没有保存的规则集配置，跳过同步")
            return null
        }

        logger.info("开始同步规则集配置到 Local Server: $savedSets")

        // 同步到 Local Server
        return selectCheckerSets(savedSets)
    }

    override fun isRememberCheckerSetsEnabled(): Boolean {
        return try {
            val settings = getSettings()
            val enabled = settings?.rememberCheckerSets ?: true // 默认启用
            logger.debug("记住规则集选择功能状态: $enabled")
            enabled
        } catch (e: Exception) {
            logger.warn("获取'记住规则集选择'配置失败，使用默认值 true: ${e.message}")
            true
        }
    }

    override fun setRememberCheckerSetsEnabled(enabled: Boolean) {
        try {
            val settings = getSettings()
            if (settings != null) {
                settings.rememberCheckerSets = enabled
                logger.info("设置'记住规则集选择'功能: $enabled")
            } else {
                logger.error("无法获取配置服务，设置失败")
            }
        } catch (e: Exception) {
            logger.error("设置'记住规则集选择'功能失败: ${e.message}", e)
        }
    }

    /**
     * 获取 PreCISettings 服务实例
     *
     * 在测试环境中可能无法获取服务，此时返回 null
     *
     * @return PreCISettings 实例或 null
     */
    private fun getSettings(): PreCISettings? {
        return try {
            PreCISettings.getInstance()
        } catch (e: Exception) {
            logger.warn("无法获取 PreCISettings 服务: ${e.message}")
            null
        }
    }

    /**
     * 显示 IDE 通知
     *
     * @param title 通知标题
     * @param content 通知内容
     * @param type 通知类型
     */
    private fun showNotification(title: String, content: String, type: NotificationType) {
        try {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("PreCI.Scan")  // 复用 Scan 通知组
                .createNotification(title, content, type)
                .notify(project)
        } catch (e: Exception) {
            logger.error("显示通知失败: ${e.message}", e)
        }
    }
}
