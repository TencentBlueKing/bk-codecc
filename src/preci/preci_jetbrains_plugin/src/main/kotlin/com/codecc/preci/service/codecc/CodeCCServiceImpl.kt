package com.codecc.preci.service.codecc

import com.codecc.preci.api.client.PreCIApiClient
import com.codecc.preci.api.model.request.RemoteDefectListRequest
import com.codecc.preci.core.http.ApiErrorHandler
import com.codecc.preci.core.http.BusinessException
import com.codecc.preci.core.http.NetworkException
import com.codecc.preci.core.http.PreCIApiException
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.util.PathHelper
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * CodeCC 远程代码检查服务实现
 *
 * 实现 [CodeCCService] 接口，提供与 CodeCC 平台交互的能力。
 * 包括获取远程任务列表和缺陷列表。
 *
 * **实现特性：**
 * - 协程支持：所有 I/O 操作在 Dispatchers.IO 中执行
 * - 详细日志：记录所有 CodeCC 操作
 * - 统一错误处理：区分业务异常、网络异常和未知异常
 * - IDE 通知：操作失败时通过 IDE 通知系统提示用户
 *
 * @property project 当前项目
 *
 * @since 1.0
 */
class CodeCCServiceImpl(private val project: Project) : CodeCCService {

    private val logger = PreCILogger.getLogger(CodeCCServiceImpl::class.java)
    private val apiClient = PreCIApiClient()

    override suspend fun getRemoteTaskList(): RemoteTaskListResult = withContext(Dispatchers.IO) {
        getRemoteTaskListBlocking()
    }

    @Suppress("TooGenericExceptionCaught")
    override fun getRemoteTaskListBlocking(): RemoteTaskListResult {
        return try {
            logger.info("开始获取远程任务列表")

            val response = runBlocking { apiClient.getRemoteTaskList() }
            val tasks = response.getTaskList()

            logger.info("成功获取远程任务列表，共 ${tasks.size} 个任务")
            tasks.forEach { task ->
                logger.debug("任务: ${task.taskId} - ${task.getDisplayName()}")
            }

            RemoteTaskListResult.Success(tasks, response)

        } catch (e: BusinessException) {
            logger.error("获取远程任务列表失败 (业务异常): ${e.message}", e)
            val handled = ApiErrorHandler.handle(e, project)
            if (handled) {
                return RemoteTaskListResult.Failure(e.message.orEmpty(), e)
            }

            val message = when (e.httpCode) {
                401 -> "认证失败：请先登录后再试"
                403 -> "权限不足：无权限获取任务列表"
                else -> "获取任务列表失败：${e.message}"
            }
            showNotification("获取远程任务列表失败", message, NotificationType.ERROR)
            RemoteTaskListResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("获取远程任务列表失败 (网络异常): ${e.message}", e)
            val message = "网络错误：${e.message}\n请检查 PreCI Local Server 是否正常运行"
            showNotification("获取远程任务列表失败", message, NotificationType.ERROR)
            RemoteTaskListResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("获取远程任务列表失败 (API 异常): ${e.message}", e)
            val message = "服务错误：${e.message}"
            showNotification("获取远程任务列表失败", message, NotificationType.ERROR)
            RemoteTaskListResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("获取远程任务列表失败 (未知异常): ${e.message}", e)
            val message = "未知错误：${e.message}"
            showNotification("获取远程任务列表失败", message, NotificationType.ERROR)
            RemoteTaskListResult.Failure(message, e)
        }
    }

    override suspend fun getRemoteDefectList(
        taskId: Long,
        dimensionList: List<String>?,
        checker: String?,
        severity: List<String>?,
        status: List<String>?,
        pageNum: Int?,
        pageSize: Int?
    ): RemoteDefectListResult = withContext(Dispatchers.IO) {
        getRemoteDefectListBlocking(taskId, dimensionList, checker, severity, status, pageNum, pageSize)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun getRemoteDefectListBlocking(
        taskId: Long,
        dimensionList: List<String>?,
        checker: String?,
        severity: List<String>?,
        status: List<String>?,
        pageNum: Int?,
        pageSize: Int?
    ): RemoteDefectListResult {
        return try {
            val projectRoot = project.basePath?.let { PathHelper.toNativePath(it) } ?: ""
            logger.info(
                "开始获取远程缺陷列表，任务ID: $taskId, 维度: $dimensionList, " +
                    "规则: $checker, 项目根目录: $projectRoot"
            )

            val request = RemoteDefectListRequest(
                projectRoot = projectRoot,
                taskIdList = listOf(taskId),
                dimensionList = dimensionList,
                checker = checker,
                severity = severity,
                status = status,
                pageNum = pageNum,
                pageSize = pageSize
            )

            val response = runBlocking { apiClient.getRemoteDefectList(request) }

            logger.info(
                "成功获取远程缺陷列表: ${response.getSummaryText()}, " +
                    "本页 ${response.getDefectList().size} 条"
            )

            RemoteDefectListResult.Success(response)

        } catch (e: BusinessException) {
            logger.error("获取远程缺陷列表失败 (业务异常): ${e.message}", e)
            val handled = ApiErrorHandler.handle(e, project)
            if (handled) {
                return RemoteDefectListResult.Failure(e.message.orEmpty(), e)
            }

            val message = when (e.httpCode) {
                401 -> "认证失败：请先登录后再试"
                403 -> "权限不足：无权限获取缺陷列表"
                else -> "获取缺陷列表失败：${e.message}"
            }
            showNotification("获取远程缺陷列表失败", message, NotificationType.ERROR)
            RemoteDefectListResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("获取远程缺陷列表失败 (网络异常): ${e.message}", e)
            val message = "网络错误：${e.message}\n请检查 PreCI Local Server 是否正常运行"
            showNotification("获取远程缺陷列表失败", message, NotificationType.ERROR)
            RemoteDefectListResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("获取远程缺陷列表失败 (API 异常): ${e.message}", e)
            val message = "服务错误：${e.message}"
            showNotification("获取远程缺陷列表失败", message, NotificationType.ERROR)
            RemoteDefectListResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("获取远程缺陷列表失败 (未知异常): ${e.message}", e)
            val message = "未知错误：${e.message}"
            showNotification("获取远程缺陷列表失败", message, NotificationType.ERROR)
            RemoteDefectListResult.Failure(message, e)
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
                .getNotificationGroup("PreCI.CodeCC")
                .createNotification(title, content, type)
                .notify(project)
        } catch (e: Exception) {
            logger.error("显示通知失败: ${e.message}", e)
        }
    }
}
