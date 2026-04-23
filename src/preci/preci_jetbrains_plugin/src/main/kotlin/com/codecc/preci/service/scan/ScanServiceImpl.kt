package com.codecc.preci.service.scan

import com.codecc.preci.api.client.PreCIApiClient
import com.codecc.preci.api.model.request.InitRequest
import com.codecc.preci.api.model.request.ScanRequest
import com.codecc.preci.core.http.ApiErrorHandler
import com.codecc.preci.core.http.BusinessException
import com.codecc.preci.core.http.NetworkException
import com.codecc.preci.core.http.PreCIApiException
import com.codecc.preci.core.http.ServerBusyException
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.server.ServerManagementService
import com.codecc.preci.service.version.UpdateResult
import com.codecc.preci.service.version.VersionService
import com.codecc.preci.util.PathHelper
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 扫描服务实现
 *
 * 实现 [ScanService] 接口，提供项目初始化和代码扫描功能。
 *
 * **实现特性：**
 * - 协程支持：所有 I/O 操作在 Dispatchers.IO 中执行
 * - 自动项目路径检测：从 IntelliJ Project 获取项目根目录
 * - 详细日志：记录所有扫描操作
 * - 用户友好：提供清晰的错误信息和 IDE 通知
 *
 * **项目初始化流程：**
 * 1. 获取项目根目录（Project.basePath）
 * 2. 调用 PreCI Local Server 的 `/task/init` 接口
 * 3. Server 推断项目根目录并创建配置文件
 * 4. 返回初始化结果
 *
 * @property project 当前项目
 *
 * @since 1.0
 */
class ScanServiceImpl(private val project: Project) : ScanService {

    private val logger = PreCILogger.getLogger(ScanServiceImpl::class.java)
    private val apiClient = PreCIApiClient()

    /**
     * HTTP 状态码常量
     */
    companion object {
        private const val HTTP_BAD_REQUEST = 400
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_INTERNAL_ERROR = 500

        // 扫描类型常量（与 preci_server_v2 保持一致）
        private const val SCAN_TYPE_FULL = 0       // 全量扫描
        private const val SCAN_TYPE_TARGET = 100   // 目标扫描
        private const val SCAN_TYPE_PRE_COMMIT = 102 // pre-commit 增量扫描
        private const val SCAN_TYPE_PRE_PUSH = 103   // pre-push 增量扫描
    }

    private fun toNativePath(path: String): String = PathHelper.toNativePath(path)

    @Suppress("TooGenericExceptionCaught", "InjectDispatcher") // 需要捕获所有异常以保证健壮性，Dispatchers.IO 在此场景合理
    override suspend fun initProject(rootPath: String?, onProgress: ((InitProgress) -> Unit)?): InitResult = withContext(Dispatchers.IO) {
        try {
            val projectBasePath = project.basePath
            if (projectBasePath == null) {
                val errorMsg = "无法获取项目根目录：Project.basePath 为 null"
                logger.error(errorMsg)
                showNotification("初始化失败", errorMsg, NotificationType.ERROR)
                return@withContext InitResult.Failure(errorMsg)
            }

            val actualRootPath = toNativePath(rootPath ?: projectBasePath)
            val nativeBasePath = toNativePath(projectBasePath)

            // --- 阶段 1：调用 /task/init ---
            logger.info("开始初始化项目: currentPath=$nativeBasePath, rootPath=$actualRootPath")
            onProgress?.invoke(InitProgress(phase = InitPhase.INITIALIZING))

            val request = InitRequest(
                currentPath = nativeBasePath,
                rootPath = actualRootPath
            )

            val response = apiClient.initProject(request)
            logger.info("项目初始化成功: rootPath=${response.rootPath}, tools=${response.tools}")

            if (response.hasUpdate) {
                logger.info("检测到可用更新: ${response.currentVersion} -> ${response.latestVersion}，将在初始化完成后自动更新")
            }

            // --- 阶段 2：逐个下载工具 ---
            val tools = response.tools
            val failedTools = mutableListOf<String>()

            if (tools.isNotEmpty()) {
                logger.info("开始下载 ${tools.size} 个工具: $tools")
                tools.forEachIndexed { index, toolName ->
                    onProgress?.invoke(
                        InitProgress(
                            phase = InitPhase.DOWNLOADING_TOOL,
                            currentTool = toolName,
                            toolIndex = index + 1,
                            totalTools = tools.size
                        )
                    )
                    try {
                        apiClient.reloadTool(toolName)
                        logger.info("工具下载成功: $toolName")
                    } catch (e: Exception) {
                        logger.warn("工具下载失败: $toolName - ${e.message.orEmpty()}", e)
                        failedTools.add(toolName)
                    }
                }
            }

            onProgress?.invoke(InitProgress(phase = InitPhase.COMPLETED, totalTools = tools.size))

            val resultMsg = buildString {
                append("项目已成功初始化\n根目录: ${response.rootPath}")
                if (tools.isNotEmpty()) {
                    append("\n工具数: ${tools.size}")
                }
                if (failedTools.isNotEmpty()) {
                    append("\n以下工具下载失败: ${failedTools.joinToString(", ")}")
                }
            }

            showNotification(
                if (failedTools.isEmpty()) "初始化成功" else "初始化完成（部分工具下载失败）",
                resultMsg,
                if (failedTools.isEmpty()) NotificationType.INFORMATION else NotificationType.WARNING
            )

            if (response.hasUpdate) {
                triggerAutoUpdate(actualRootPath)
            }

            InitResult.Success(response)

        } catch (e: BusinessException) {
            logger.error("项目初始化失败: ${e.message.orEmpty()}", e)

            val handled = ApiErrorHandler.handle(e, project)
            if (handled) {
                val message = e.message.orEmpty()
                return@withContext InitResult.Failure(message, e)
            }

            val message = when (e.httpCode) {
                HTTP_BAD_REQUEST -> "初始化失败：请求参数无效，请检查项目路径"
                HTTP_UNAUTHORIZED -> "初始化失败：未登录或认证已过期，请先登录"
                HTTP_FORBIDDEN -> "初始化失败：权限不足，无法访问该项目"
                HTTP_INTERNAL_ERROR -> "初始化失败：服务器内部错误，${e.message.orEmpty()}"
                else -> "初始化失败：${e.message.orEmpty()}"
            }
            showNotification("初始化失败", message, NotificationType.ERROR)
            InitResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("项目初始化网络错误: ${e.message.orEmpty()}", e)
            val message = formatNetworkError(e)
            showNotification("初始化失败", message, NotificationType.ERROR)
            InitResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("项目初始化 API 错误: ${e.message.orEmpty()}", e)
            val message = "服务错误：${e.message.orEmpty()}"
            showNotification("初始化失败", message, NotificationType.ERROR)
            InitResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("项目初始化异常: ${e.message.orEmpty()}", e)
            val message = "未知错误：${e.message.orEmpty()}"
            showNotification("初始化失败", message, NotificationType.ERROR)
            InitResult.Failure(message, e)
        }
    }

    @Suppress("TooGenericExceptionCaught", "InjectDispatcher") // 需要捕获所有异常以保证健壮性，Dispatchers.IO 在此场景合理
    override suspend fun fullScan(rootDir: String?): ScanResult = withContext(Dispatchers.IO) {
        try {
            // 获取项目根目录
            val projectBasePath = project.basePath
            if (projectBasePath == null) {
                val errorMsg = "无法获取项目根目录：Project.basePath 为 null"
                logger.error(errorMsg)
                showNotification("全量扫描失败", errorMsg, NotificationType.ERROR)
                return@withContext ScanResult.Failure(errorMsg)
            }

            // 使用传入的 rootDir 或项目根目录
            val actualRootDir = toNativePath(rootDir ?: projectBasePath)

            logger.info("开始执行全量扫描: rootDir=$actualRootDir")

            // 调用 API
            val request = com.codecc.preci.api.model.request.ScanRequest(
                scanType = SCAN_TYPE_FULL,
                paths = null,
                rootDir = actualRootDir
            )

            val response = apiClient.scan(request)
            logger.info("全量扫描启动成功: tools=${response.tools}, scanFileNum=${response.scanFileNum}")

            showNotification(
                "全量扫描已启动",
                "扫描工具: ${response.tools.joinToString(", ")}\n${response.message}",
                NotificationType.INFORMATION
            )

            ScanResult.Success(response)

        } catch (e: BusinessException) {
            logger.error("全量扫描失败: ${e.message.orEmpty()}", e)
            
            // 尝试自动处理错误（如自动登录、提示绑定项目）
            val handled = ApiErrorHandler.handle(e, project)
            
            // 如果错误已被处理（如项目 ID 无效），直接返回失败结果，不再显示额外的错误提示
            if (handled) {
                val message = e.message.orEmpty()
                return@withContext ScanResult.Failure(message, e)
            }
            
            val message = when (e.httpCode) {
                HTTP_BAD_REQUEST -> "扫描失败：请求参数无效，请检查项目是否已初始化"
                HTTP_UNAUTHORIZED -> "扫描失败：未登录或认证已过期，请先登录"
                HTTP_FORBIDDEN -> "扫描失败：权限不足，无法访问该项目"
                HTTP_INTERNAL_ERROR -> "扫描失败：服务器内部错误，${e.message.orEmpty()}"
                else -> "扫描失败：${e.message.orEmpty()}"
            }
            showNotification("全量扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("全量扫描网络错误: ${e.message.orEmpty()}", e)
            val message = formatNetworkError(e)
            showNotification("全量扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("全量扫描 API 错误: ${e.message.orEmpty()}", e)
            val message = "服务错误：${e.message.orEmpty()}"
            showNotification("全量扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("全量扫描异常: ${e.message.orEmpty()}", e)
            val message = "未知错误：${e.message.orEmpty()}"
            showNotification("全量扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)
        }
    }

    @Suppress("TooGenericExceptionCaught", "InjectDispatcher") // 需要捕获所有异常以保证健壮性，Dispatchers.IO 在此场景合理
    override suspend fun targetScan(paths: List<String>, rootDir: String?): ScanResult = withContext(Dispatchers.IO) {
        try {
            // 验证 paths 参数
            if (paths.isEmpty()) {
                val errorMsg = "目标扫描失败：paths 参数不能为空"
                logger.error(errorMsg)
                showNotification("目标扫描失败", errorMsg, NotificationType.ERROR)
                return@withContext ScanResult.Failure(errorMsg)
            }

            // 获取项目根目录
            val projectBasePath = project.basePath
            if (projectBasePath == null) {
                val errorMsg = "无法获取项目根目录：Project.basePath 为 null"
                logger.error(errorMsg)
                showNotification("目标扫描失败", errorMsg, NotificationType.ERROR)
                return@withContext ScanResult.Failure(errorMsg)
            }

            // 使用传入的 rootDir 或项目根目录
            val actualRootDir = toNativePath(rootDir ?: projectBasePath)
            val nativePaths = paths.map { toNativePath(it) }

            logger.warn("开始执行目标扫描: rootDir=$actualRootDir, paths=$nativePaths")

            // 调用 API
            val request = com.codecc.preci.api.model.request.ScanRequest(
                scanType = SCAN_TYPE_TARGET,
                paths = nativePaths,
                rootDir = actualRootDir
            )

            val response = apiClient.scan(request)
            logger.info("目标扫描启动成功: tools=${response.tools}, scanFileNum=${response.scanFileNum}")

            showNotification(
                "目标扫描已启动",
                "扫描工具: ${response.tools.joinToString(", ")}\n扫描文件数: ${response.scanFileNum}\n${response.message}",
                NotificationType.INFORMATION
            )

            ScanResult.Success(response)

        } catch (e: BusinessException) {
            logger.error("目标扫描失败: ${e.message.orEmpty()}", e)
            
            // 尝试自动处理错误（如自动登录、提示绑定项目）
            val handled = ApiErrorHandler.handle(e, project)
            
            // 如果错误已被处理（如项目 ID 无效），直接返回失败结果，不再显示额外的错误提示
            if (handled) {
                val message = e.message.orEmpty()
                return@withContext ScanResult.Failure(message, e)
            }
            
            val message = when (e.httpCode) {
                HTTP_BAD_REQUEST -> "扫描失败：请求参数无效，请检查项目是否已初始化或路径是否正确"
                HTTP_UNAUTHORIZED -> "扫描失败：未登录或认证已过期，请先登录"
                HTTP_FORBIDDEN -> "扫描失败：权限不足，无法访问该项目"
                HTTP_INTERNAL_ERROR -> "扫描失败：服务器内部错误，${e.message.orEmpty()}"
                else -> "扫描失败：${e.message.orEmpty()}"
            }
            showNotification("目标扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("目标扫描网络错误: ${e.message.orEmpty()}", e)
            val message = formatNetworkError(e)
            showNotification("目标扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("目标扫描 API 错误: ${e.message.orEmpty()}", e)
            val message = "服务错误：${e.message.orEmpty()}"
            showNotification("目标扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("目标扫描异常: ${e.message.orEmpty()}", e)
            val message = "未知错误：${e.message.orEmpty()}"
            showNotification("目标扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)
        }
    }

    @Suppress("TooGenericExceptionCaught", "InjectDispatcher") // 需要捕获所有异常以保证健壮性，Dispatchers.IO 在此场景合理
    override suspend fun preCommitScan(rootDir: String?): ScanResult = withContext(Dispatchers.IO) {
        try {
            // 获取项目根目录
            val projectBasePath = project.basePath
            if (projectBasePath == null) {
                val errorMsg = "无法获取项目根目录：Project.basePath 为 null"
                logger.error(errorMsg)
                showNotification("Pre-Commit 扫描失败", errorMsg, NotificationType.ERROR)
                return@withContext ScanResult.Failure(errorMsg)
            }

            // 使用传入的 rootDir 或项目根目录
            val actualRootDir = toNativePath(rootDir ?: projectBasePath)

            logger.info("开始执行 pre-commit 增量扫描: rootDir=$actualRootDir")

            // 调用 API
            val request = com.codecc.preci.api.model.request.ScanRequest(
                scanType = SCAN_TYPE_PRE_COMMIT,
                paths = null,
                rootDir = actualRootDir
            )

            val response = apiClient.scan(request)
            logger.info("Pre-commit 扫描启动成功: tools=${response.tools}, scanFileNum=${response.scanFileNum}")

            showNotification(
                "Pre-Commit 扫描已启动",
                "扫描工具: ${response.tools.joinToString(", ")}\n扫描文件数: ${response.scanFileNum}\n${response.message}",
                NotificationType.INFORMATION
            )

            ScanResult.Success(response)

        } catch (e: BusinessException) {
            logger.error("Pre-commit 扫描失败: ${e.message.orEmpty()}", e)
            
            // 尝试自动处理错误（如自动登录、提示绑定项目）
            val handled = ApiErrorHandler.handle(e, project)
            
            // 如果错误已被处理（如项目 ID 无效），直接返回失败结果，不再显示额外的错误提示
            if (handled) {
                val message = e.message.orEmpty()
                return@withContext ScanResult.Failure(message, e)
            }
            
            val message = when (e.httpCode) {
                HTTP_BAD_REQUEST -> "扫描失败：请求参数无效，请检查项目是否已初始化或 Git 暂存区是否有变更"
                HTTP_UNAUTHORIZED -> "扫描失败：未登录或认证已过期，请先登录"
                HTTP_FORBIDDEN -> "扫描失败：权限不足，无法访问该项目"
                HTTP_INTERNAL_ERROR -> "扫描失败：服务器内部错误，${e.message.orEmpty()}"
                else -> "扫描失败：${e.message.orEmpty()}"
            }
            showNotification("Pre-Commit 扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("Pre-commit 扫描网络错误: ${e.message.orEmpty()}", e)
            val message = formatNetworkError(e)
            showNotification("Pre-Commit 扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("Pre-commit 扫描 API 错误: ${e.message.orEmpty()}", e)
            val message = "服务错误：${e.message.orEmpty()}"
            showNotification("Pre-Commit 扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("Pre-commit 扫描异常: ${e.message.orEmpty()}", e)
            val message = "未知错误：${e.message.orEmpty()}"
            showNotification("Pre-Commit 扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)
        }
    }

    @Suppress("TooGenericExceptionCaught", "InjectDispatcher") // 需要捕获所有异常以保证健壮性，Dispatchers.IO 在此场景合理
    override suspend fun prePushScan(rootDir: String?): ScanResult = withContext(Dispatchers.IO) {
        try {
            // 获取项目根目录
            val projectBasePath = project.basePath
            if (projectBasePath == null) {
                val errorMsg = "无法获取项目根目录：Project.basePath 为 null"
                logger.error(errorMsg)
                showNotification("Pre-Push 扫描失败", errorMsg, NotificationType.ERROR)
                return@withContext ScanResult.Failure(errorMsg)
            }

            // 使用传入的 rootDir 或项目根目录
            val actualRootDir = toNativePath(rootDir ?: projectBasePath)

            logger.info("开始执行 pre-push 增量扫描: rootDir=$actualRootDir")

            // 调用 API
            val request = com.codecc.preci.api.model.request.ScanRequest(
                scanType = SCAN_TYPE_PRE_PUSH,
                paths = null,
                rootDir = actualRootDir
            )

            val response = apiClient.scan(request)
            logger.info("Pre-push 扫描启动成功: tools=${response.tools}, scanFileNum=${response.scanFileNum}")

            showNotification(
                "Pre-Push 扫描已启动",
                "扫描工具: ${response.tools.joinToString(", ")}\n扫描文件数: ${response.scanFileNum}\n${response.message}",
                NotificationType.INFORMATION
            )

            ScanResult.Success(response)

        } catch (e: BusinessException) {
            logger.error("Pre-push 扫描失败: ${e.message.orEmpty()}", e)
            
            // 尝试自动处理错误（如自动登录、提示绑定项目）
            val handled = ApiErrorHandler.handle(e, project)
            
            // 如果错误已被处理（如项目 ID 无效），直接返回失败结果，不再显示额外的错误提示
            if (handled) {
                val message = e.message.orEmpty()
                return@withContext ScanResult.Failure(message, e)
            }
            
            val message = when (e.httpCode) {
                HTTP_BAD_REQUEST -> "扫描失败：请求参数无效，请检查项目是否已初始化或是否有未推送的提交"
                HTTP_UNAUTHORIZED -> "扫描失败：未登录或认证已过期，请先登录"
                HTTP_FORBIDDEN -> "扫描失败：权限不足，无法访问该项目"
                HTTP_INTERNAL_ERROR -> "扫描失败：服务器内部错误，${e.message.orEmpty()}"
                else -> "扫描失败：${e.message.orEmpty()}"
            }
            showNotification("Pre-Push 扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("Pre-push 扫描网络错误: ${e.message.orEmpty()}", e)
            val message = formatNetworkError(e)
            showNotification("Pre-Push 扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("Pre-push 扫描 API 错误: ${e.message.orEmpty()}", e)
            val message = "服务错误：${e.message.orEmpty()}"
            showNotification("Pre-Push 扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("Pre-push 扫描异常: ${e.message.orEmpty()}", e)
            val message = "未知错误：${e.message.orEmpty()}"
            showNotification("Pre-Push 扫描失败", message, NotificationType.ERROR)
            ScanResult.Failure(message, e)
        }
    }

    @Suppress("TooGenericExceptionCaught", "InjectDispatcher") // 需要捕获所有异常以保证健壮性，Dispatchers.IO 在此场景合理
    override suspend fun getScanProgress(): ScanProgressResult = withContext(Dispatchers.IO) {
        try {
            logger.info("开始查询扫描进度")

            // 调用 API
            val response = apiClient.getScanProgress()
            logger.info("扫描进度查询成功: status=${response.status}, projectRoot=${response.projectRoot}")

            ScanProgressResult.Success(response)

        } catch (e: BusinessException) {
            logger.error("扫描进度查询失败: ${e.message.orEmpty()}", e)
            val message = when (e.httpCode) {
                HTTP_BAD_REQUEST -> "进度查询失败：请求参数无效"
                HTTP_UNAUTHORIZED -> "进度查询失败：未登录或认证已过期，请先登录"
                HTTP_FORBIDDEN -> "进度查询失败：权限不足"
                HTTP_INTERNAL_ERROR -> "进度查询失败：服务器内部错误，${e.message.orEmpty()}"
                else -> "进度查询失败：${e.message.orEmpty()}"
            }
            ScanProgressResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("扫描进度查询网络错误: ${e.message.orEmpty()}", e)
            val message = formatNetworkError(e)
            ScanProgressResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("扫描进度查询 API 错误: ${e.message.orEmpty()}", e)
            val message = "服务错误：${e.message.orEmpty()}"
            ScanProgressResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("扫描进度查询异常: ${e.message.orEmpty()}", e)
            val message = "未知错误：${e.message.orEmpty()}"
            ScanProgressResult.Failure(message, e)
        }
    }

    @Suppress("TooGenericExceptionCaught", "InjectDispatcher") // 需要捕获所有异常以保证健壮性，Dispatchers.IO 在此场景合理
    override suspend fun getScanResult(path: String?): ScanResultQueryResult = withContext(Dispatchers.IO) {
        try {
            // 获取项目根目录
            val projectBasePath = project.basePath
            if (projectBasePath == null) {
                val errorMsg = "无法获取项目根目录：Project.basePath 为 null"
                logger.error(errorMsg)
                return@withContext ScanResultQueryResult.Failure(errorMsg)
            }

            // 使用传入的 path 或项目根目录
            val actualPath = toNativePath(path ?: projectBasePath)

            logger.info("开始查询扫描结果: path=$actualPath")

            // 调用 API
            val request = com.codecc.preci.api.model.request.ScanResultRequest(
                path = actualPath
            )

            val response = apiClient.getScanResult(request)
            logger.info("扫描结果查询成功: defects.size=${response.getDefectList().size}")

            ScanResultQueryResult.Success(response)

        } catch (e: BusinessException) {
            logger.error("扫描结果查询失败: ${e.message.orEmpty()}", e)
            val message = when (e.httpCode) {
                HTTP_BAD_REQUEST -> "结果查询失败：请求参数无效，请检查路径是否正确"
                HTTP_UNAUTHORIZED -> "结果查询失败：未登录或认证已过期，请先登录"
                HTTP_FORBIDDEN -> "结果查询失败：权限不足"
                HTTP_INTERNAL_ERROR -> "结果查询失败：服务器内部错误，${e.message.orEmpty()}"
                else -> "结果查询失败：${e.message.orEmpty()}"
            }
            ScanResultQueryResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("扫描结果查询网络错误: ${e.message.orEmpty()}", e)
            val message = formatNetworkError(e)
            ScanResultQueryResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("扫描结果查询 API 错误: ${e.message.orEmpty()}", e)
            val message = "服务错误：${e.message.orEmpty()}"
            ScanResultQueryResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("扫描结果查询异常: ${e.message.orEmpty()}", e)
            val message = "未知错误：${e.message.orEmpty()}"
            ScanResultQueryResult.Failure(message, e)
        }
    }

    @Suppress("TooGenericExceptionCaught", "InjectDispatcher") // 需要捕获所有异常以保证健壮性，Dispatchers.IO 在此场景合理
    override suspend fun cancelScan(): CancelScanResult = withContext(Dispatchers.IO) {
        try {
            logger.info("开始取消扫描")

            // 调用 API
            val response = apiClient.cancelScan()
            logger.info("扫描取消成功: projectRoot=${response.projectRoot}")

            showNotification(
                "扫描已取消",
                "已成功取消扫描任务\n项目: ${response.projectRoot}",
                NotificationType.INFORMATION
            )

            CancelScanResult.Success(response.projectRoot)

        } catch (e: BusinessException) {
            logger.error("扫描取消失败: ${e.message.orEmpty()}", e)
            val message = when (e.httpCode) {
                HTTP_BAD_REQUEST -> "取消失败：没有正在进行的扫描任务"
                HTTP_UNAUTHORIZED -> "取消失败：未登录或认证已过期，请先登录"
                HTTP_FORBIDDEN -> "取消失败：权限不足"
                HTTP_INTERNAL_ERROR -> "取消失败：服务器内部错误，${e.message.orEmpty()}"
                else -> "取消失败：${e.message.orEmpty()}"
            }
            showNotification("扫描取消失败", message, NotificationType.ERROR)
            CancelScanResult.Failure(message, e)

        } catch (e: NetworkException) {
            logger.error("扫描取消网络错误: ${e.message.orEmpty()}", e)
            val message = formatNetworkError(e)
            showNotification("扫描取消失败", message, NotificationType.ERROR)
            CancelScanResult.Failure(message, e)

        } catch (e: PreCIApiException) {
            logger.error("扫描取消 API 错误: ${e.message.orEmpty()}", e)
            val message = "服务错误：${e.message.orEmpty()}"
            showNotification("扫描取消失败", message, NotificationType.ERROR)
            CancelScanResult.Failure(message, e)

        } catch (e: Exception) {
            logger.error("扫描取消异常: ${e.message.orEmpty()}", e)
            val message = "未知错误：${e.message.orEmpty()}"
            showNotification("扫描取消失败", message, NotificationType.ERROR)
            CancelScanResult.Failure(message, e)
        }
    }

    /**
     * 初始化检测到新版本后，自动在后台执行 `preci update` 更新，更新完成后重新初始化。
     *
     * 完整流程：performUpdate → 等待 3 秒 → 启动新 server → 重新 initProject。
     * 重新初始化时新版本 server 的 hasUpdate 应为 false，不会产生无限循环。
     * 互斥由 [VersionServiceImpl.performUpdate] 内部的 AtomicBoolean 保证。
     *
     * @param rootPath 当前项目根目录路径，用于更新后重新初始化
     */
    private fun triggerAutoUpdate(rootPath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val versionService = VersionService.getInstance()
                when (val updateResult = versionService.performUpdate()) {
                    is UpdateResult.Success -> {
                        logger.info("自动更新执行完成，等待服务重启...")
                        delay(3000)

                        try {
                            val serverService = ServerManagementService.getInstance(project)
                            serverService.startServer()
                            logger.info("自动更新后服务已重启")
                        } catch (e: Exception) {
                            logger.error("自动更新后服务重启失败: ${e.message}", e)
                            showNotification(
                                "PreCI CLI 已更新",
                                "更新完成，但服务重启失败，请手动执行 preci server start",
                                NotificationType.WARNING
                            )
                            return@launch
                        }

                        logger.info("自动更新后开始重新初始化项目: rootPath=$rootPath")
                        val reInitResult = initProject(rootPath)
                        when (reInitResult) {
                            is InitResult.Success -> {
                                logger.info("自动更新后重新初始化成功")
                                showNotification(
                                    "PreCI CLI 已自动更新",
                                    "更新完成，项目已重新初始化",
                                    NotificationType.INFORMATION
                                )
                            }
                            is InitResult.Failure -> {
                                logger.error("自动更新后重新初始化失败: ${reInitResult.message}")
                                showNotification(
                                    "PreCI CLI 已更新",
                                    "更新完成，但项目重新初始化失败: ${reInitResult.message}",
                                    NotificationType.WARNING
                                )
                            }
                        }
                    }
                    is UpdateResult.Failure -> {
                        logger.warn("自动更新跳过或失败: ${updateResult.message}")
                    }
                }
            } catch (e: Exception) {
                logger.error("自动更新异常: ${e.message}", e)
            }
        }
    }

    private fun formatNetworkError(e: NetworkException): String {
        return if (e is ServerBusyException) {
            "服务响应超时：${e.message.orEmpty()}"
        } else {
            "网络错误：${e.message.orEmpty()}\n请检查 PreCI Local Server 是否正常运行"
        }
    }

    /**
     * 显示 IDE 通知
     *
     * @param title 通知标题
     * @param content 通知内容
     * @param type 通知类型
     */
    @Suppress("TooGenericExceptionCaught") // 通知失败不应影响主流程，需要捕获所有异常
    private fun showNotification(title: String, content: String, type: NotificationType) {
        try {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("PreCI.Scan")
                .createNotification(title, content, type)
                .notify(project)
        } catch (e: Exception) {
            logger.error("显示通知失败: ${e.message.orEmpty()}", e)
        }
    }
}

