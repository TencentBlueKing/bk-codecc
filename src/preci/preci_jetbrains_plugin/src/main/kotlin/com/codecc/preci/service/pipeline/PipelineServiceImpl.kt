package com.codecc.preci.service.pipeline

import com.codecc.preci.api.client.PreCIApiClient
import com.codecc.preci.api.model.request.StartBuildRequest
import com.codecc.preci.core.http.ApiErrorHandler
import com.codecc.preci.core.http.BusinessException
import com.codecc.preci.core.http.NetworkException
import com.codecc.preci.core.http.PreCIApiException
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.util.PathHelper
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/**
 * PAC 流水线服务实现
 *
 * 实现 [PipelineService] 接口，提供流水线构建管理能力。
 *
 * **实现特性：**
 * - 协程支持：所有 I/O 操作通过 PreCIApiClient 在 Dispatchers.IO 中执行
 * - 详细日志：记录所有流水线操作
 * - 统一错误处理：区分业务异常、网络异常和未知异常
 * - IDE 通知：操作失败时通过 IDE 通知系统提示用户
 *
 * @property project 当前项目
 *
 * @since 1.0
 */
class PipelineServiceImpl(private val project: Project) : PipelineService {

    private val logger = PreCILogger.getLogger(PipelineServiceImpl::class.java)
    private val apiClient = PreCIApiClient()

    /**
     * 获取当前项目的根目录路径（转换为本地路径格式）
     */
    private fun getRootPath(): String {
        return project.basePath?.let { PathHelper.toNativePath(it) } ?: ""
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getBuildHistory(): BuildHistoryResult {
        return try {
            val rootPath = getRootPath()
            logger.info("开始获取流水线构建历史，项目路径: $rootPath")

            val response = apiClient.getPipelineBuildHistory(rootPath)

            val buildList = response.buildsOrEmpty()
            logger.info("成功获取流水线构建历史，共 ${buildList.size} 条记录")
            BuildHistoryResult.Success(buildList)

        } catch (e: BusinessException) {
            logger.error("获取构建历史失败 (业务异常): ${e.message}", e)
            val handled = ApiErrorHandler.handle(e, project)
            if (handled) {
                return BuildHistoryResult.Failure(e.message.orEmpty(), e)
            }
            val message = "获取构建历史失败：${e.message}"
            showNotification("获取构建历史失败", message, NotificationType.ERROR)
            BuildHistoryResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("获取构建历史失败 (网络异常): ${e.message}", e)
            val message = "网络错误：${e.message}\n请检查 PreCI Local Server 是否正常运行"
            showNotification("获取构建历史失败", message, NotificationType.ERROR)
            BuildHistoryResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("获取构建历史失败 (API 异常): ${e.message}", e)
            val message = "服务错误：${e.message}"
            showNotification("获取构建历史失败", message, NotificationType.ERROR)
            BuildHistoryResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("获取构建历史失败 (未知异常): ${e.message}", e)
            val message = "未知错误：${e.message}"
            showNotification("获取构建历史失败", message, NotificationType.ERROR)
            BuildHistoryResult.Failure(message, e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun startBuild(): StartBuildResult {
        return try {
            val rootPath = getRootPath()
            logger.info("开始触发流水线构建，项目路径: $rootPath")

            val request = StartBuildRequest(rootPath = rootPath)
            val response = apiClient.startPipelineBuild(request)

            logger.info("成功触发流水线构建，buildId: ${response.buildId}")
            showNotification("触发构建成功", "构建已启动，buildId: ${response.buildId}", NotificationType.INFORMATION)
            StartBuildResult.Success(response.buildId)

        } catch (e: BusinessException) {
            logger.error("触发构建失败 (业务异常): ${e.message}", e)
            val handled = ApiErrorHandler.handle(e, project)
            if (handled) {
                return StartBuildResult.Failure(e.message.orEmpty(), e)
            }
            val message = "触发构建失败：${e.message}"
            showNotification("触发构建失败", message, NotificationType.ERROR)
            StartBuildResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("触发构建失败 (网络异常): ${e.message}", e)
            val message = "网络错误：${e.message}\n请检查 PreCI Local Server 是否正常运行"
            showNotification("触发构建失败", message, NotificationType.ERROR)
            StartBuildResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("触发构建失败 (API 异常): ${e.message}", e)
            val message = "服务错误：${e.message}"
            showNotification("触发构建失败", message, NotificationType.ERROR)
            StartBuildResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("触发构建失败 (未知异常): ${e.message}", e)
            val message = "未知错误：${e.message}"
            showNotification("触发构建失败", message, NotificationType.ERROR)
            StartBuildResult.Failure(message, e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getBuildLogs(buildId: String, start: Long): BuildLogsResult {
        return try {
            val rootPath = getRootPath()
            logger.debug("获取构建日志，buildId: $buildId, start: $start")

            val response = apiClient.getPipelineBuildLogs(rootPath, buildId, start)

            val logList = response.logsOrEmpty()
            logger.debug("成功获取构建日志，共 ${logList.size} 行，finished: ${response.finished}")
            BuildLogsResult.Success(logList, response.finished)

        } catch (e: BusinessException) {
            logger.error("获取构建日志失败 (业务异常): ${e.message}", e)
            val handled = ApiErrorHandler.handle(e, project)
            if (handled) {
                return BuildLogsResult.Failure(e.message.orEmpty(), e)
            }
            BuildLogsResult.Failure("获取构建日志失败：${e.message}", e)

        } catch (e: NetworkException) {
            logger.error("获取构建日志失败 (网络异常): ${e.message}", e)
            BuildLogsResult.Failure("网络错误：${e.message}", e)

        } catch (e: PreCIApiException) {
            logger.error("获取构建日志失败 (API 异常): ${e.message}", e)
            BuildLogsResult.Failure("服务错误：${e.message}", e)

        } catch (e: Exception) {
            logger.error("获取构建日志失败 (未知异常): ${e.message}", e)
            BuildLogsResult.Failure("未知错误：${e.message}", e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getBuildDetail(buildId: String): BuildDetailResult {
        return try {
            val rootPath = getRootPath()
            logger.info("获取构建详情，buildId: $buildId")

            val response = apiClient.getPipelineBuildDetail(rootPath, buildId)

            logger.info("成功获取构建详情，状态: ${response.status}")
            BuildDetailResult.Success(response)

        } catch (e: BusinessException) {
            logger.error("获取构建详情失败 (业务异常): ${e.message}", e)
            val handled = ApiErrorHandler.handle(e, project)
            if (handled) {
                return BuildDetailResult.Failure(e.message.orEmpty(), e)
            }
            val message = "获取构建详情失败：${e.message}"
            showNotification("获取构建详情失败", message, NotificationType.ERROR)
            BuildDetailResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("获取构建详情失败 (网络异常): ${e.message}", e)
            val message = "网络错误：${e.message}\n请检查 PreCI Local Server 是否正常运行"
            showNotification("获取构建详情失败", message, NotificationType.ERROR)
            BuildDetailResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("获取构建详情失败 (API 异常): ${e.message}", e)
            val message = "服务错误：${e.message}"
            showNotification("获取构建详情失败", message, NotificationType.ERROR)
            BuildDetailResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("获取构建详情失败 (未知异常): ${e.message}", e)
            val message = "未知错误：${e.message}"
            showNotification("获取构建详情失败", message, NotificationType.ERROR)
            BuildDetailResult.Failure(message, e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun stopBuild(buildId: String): StopBuildResult {
        return try {
            val rootPath = getRootPath()
            logger.info("停止构建，buildId: $buildId")

            apiClient.stopPipelineBuild(rootPath, buildId)

            logger.info("成功停止构建，buildId: $buildId")
            showNotification("停止构建成功", "构建 $buildId 已停止", NotificationType.INFORMATION)
            StopBuildResult.Success

        } catch (e: BusinessException) {
            logger.error("停止构建失败 (业务异常): ${e.message}", e)
            val handled = ApiErrorHandler.handle(e, project)
            if (handled) {
                return StopBuildResult.Failure(e.message.orEmpty(), e)
            }
            val message = "停止构建失败：${e.message}"
            showNotification("停止构建失败", message, NotificationType.ERROR)
            StopBuildResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("停止构建失败 (网络异常): ${e.message}", e)
            val message = "网络错误：${e.message}\n请检查 PreCI Local Server 是否正常运行"
            showNotification("停止构建失败", message, NotificationType.ERROR)
            StopBuildResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("停止构建失败 (API 异常): ${e.message}", e)
            val message = "服务错误：${e.message}"
            showNotification("停止构建失败", message, NotificationType.ERROR)
            StopBuildResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("停止构建失败 (未知异常): ${e.message}", e)
            val message = "未知错误：${e.message}"
            showNotification("停止构建失败", message, NotificationType.ERROR)
            StopBuildResult.Failure(message, e)
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
                .getNotificationGroup("PreCI.Pipeline")
                .createNotification(title, content, type)
                .notify(project)
        } catch (e: Exception) {
            logger.error("显示通知失败: ${e.message}", e)
        }
    }
}
