package com.codecc.preci.api.client

import com.codecc.preci.api.model.request.*
import com.codecc.preci.api.model.response.*
import com.codecc.preci.api.model.response.RemoteDefectListResponse
import com.codecc.preci.api.model.response.RemoteTaskListResponse
import com.codecc.preci.core.config.PreCISettings
import com.codecc.preci.core.http.*
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.util.PreCIPortDetector
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException as KSerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * PreCI API 客户端
 *
 * 封装与 PreCI Local Server 的所有 HTTP 通信，提供类型安全的 API 调用接口。
 *
 * **功能特性：**
 * - 自动端口检测（通过 `preci port` 命令）
 * - 统一的错误处理和异常封装
 * - 自动重试机制（根据配置，默认重试 3 次）
 * - 请求/响应日志记录（根据配置启用调试日志）
 * - 协程支持（所有方法使用 suspend）
 * - 从配置读取超时和重试参数
 *
 * **使用示例：**
 * ```kotlin
 * val client = PreCIApiClient()
 *
 * // 登录
 * val loginResponse = client.login(LoginRequest("pin:token", "projectId"))
 *
 * // 扫描
 * val scanResponse = client.scan(ScanRequest(scanType = 1))
 * ```
 *
 * @since 1.0
 */
class PreCIApiClient {
    private val logger = PreCILogger.getLogger(PreCIApiClient::class.java)

    /**
     * 协程间协调的自动启动结果。非 null 且 isActive 表示正在启动中。
     * 使用 CompletableDeferred 代替 flag + polling，避免持锁等待导致死锁。
     */
    @Volatile
    private var autoStartDeferred: CompletableDeferred<Boolean>? = null

    /**
     * 用于协程安全的服务启动互斥锁（仅短暂持有，用于检查/更新 autoStartDeferred）
     */
    private val autoStartMutex = Mutex()

    /**
     * JSON 序列化/反序列化配置
     */
    private val json = Json {
        ignoreUnknownKeys = true // 忽略未知字段，增强兼容性
        isLenient = true // 宽松模式，允许非标准 JSON
        encodeDefaults = true // 编码默认值
    }

    /**
     * HTTP 客户端配置
     *
     * 从 PreCISettings 读取超时和重试配置
     *
     * **拦截器顺序（重要）：**
     * 1. ServerAutoStartInterceptor：检测连接失败并自动启动服务（最外层，第一优先级）
     * 2. AuthRetryInterceptor：检测 100005 错误并自动登录重试（第二优先级）
     * 3. LoggingInterceptor：记录请求日志
     * 4. RetryInterceptor：网络异常重试（最内层）
     */
    private val httpClient: OkHttpClient
        get() {
            val settings = try {
                PreCISettings.getInstance()
            } catch (e: Exception) {
                null // 在测试环境中可能无法获取服务
            }

            val requestTimeout = settings?.requestTimeout ?: 30
            val maxRetries = settings?.maxRetries ?: 3

            return OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS) // 连接超时固定 10 秒
                .readTimeout(requestTimeout.toLong(), TimeUnit.SECONDS) // 从配置读取或使用默认值
                .writeTimeout(requestTimeout.toLong(), TimeUnit.SECONDS) // 从配置读取或使用默认值
                .addInterceptor(ServerAutoStartInterceptor()) // 服务自动启动拦截器（最外层，第一优先级）
                .addInterceptor(AuthRetryInterceptor()) // 认证自动重试拦截器（第二优先级）
                .addInterceptor(LoggingInterceptor()) // 日志拦截器
                .addInterceptor(RetryInterceptor(maxRetries = maxRetries)) // 从配置读取重试次数或使用默认值
                .build()
        }

    /**
     * JSON Content-Type
     */
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * 获取 Local Server 的基础 URL
     *
     * @return 基础 URL，格式：http://localhost:端口号
     * @throws ServerNotRunningException 如果 Server 未运行
     */
    private fun getBaseUrl(): String {
        val port = PreCIPortDetector.getServerPort()
        return "http://localhost:$port"
    }

    /**
     * 执行 HTTP GET 请求
     *
     * 支持自动服务启动：如果检测到 Server 未运行，自动尝试启动服务
     *
     * @param path API 路径
     * @return 响应体字符串
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常（4xx/5xx）
     */
    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}$path"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .build()

            executeRequest(request)
        } catch (e: ServerNotRunningException) {
            // Server 未运行，尝试自动启动
            handleServerNotRunning(e, path, isPost = false, body = null)
        }
    }

    /**
     * 执行 HTTP POST 请求
     *
     * 支持自动服务启动：如果检测到 Server 未运行，自动尝试启动服务
     *
     * @param path API 路径
     * @param body 请求体（JSON 字符串）
     * @param readTimeoutSeconds 自定义读超时（秒），为 null 时使用默认超时
     * @return 响应体字符串
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常（4xx/5xx）
     */
    private suspend fun post(
        path: String,
        body: String,
        readTimeoutSeconds: Long? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}$path"
            val requestBody = body.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val client = if (readTimeoutSeconds != null) {
                httpClient.newBuilder()
                    .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                    .writeTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                    .build()
            } else {
                httpClient
            }

            executeRequest(request, client)
        } catch (e: ServerNotRunningException) {
            // Server 未运行，尝试自动启动
            handleServerNotRunning(e, path, isPost = true, body = body)
        }
    }

    /**
     * 执行 HTTP DELETE 请求
     *
     * 支持自动服务启动：如果检测到 Server 未运行，自动尝试启动服务
     *
     * @param path API 路径
     * @return 响应体字符串
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常（4xx/5xx）
     */
    private suspend fun delete(path: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}$path"
            val request = Request.Builder()
                .url(url)
                .delete()
                .addHeader("Content-Type", "application/json")
                .build()

            executeRequest(request)
        } catch (e: ServerNotRunningException) {
            handleServerNotRunning(e, path, isPost = false, body = null, isDelete = true)
        }
    }

    /**
     * 处理 Server 未运行的情况
     *
     * 自动启动 Server 并重试请求。使用防重入机制避免多个请求同时触发启动。
     *
     * @param exception 原始异常
     * @param path API 路径
     * @param isPost 是否为 POST 请求
     * @param body POST 请求的请求体（如果是 GET 则为 null）
     * @return 响应体字符串
     * @throws NetworkException 如果启动失败或重试失败
     */
    private suspend fun handleServerNotRunning(
        exception: ServerNotRunningException,
        path: String,
        isPost: Boolean,
        body: String?,
        isDelete: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        logger.warn("Server not running detected: ${exception.message}, attempting auto-start")

        val shouldStart: Boolean
        val deferred: CompletableDeferred<Boolean>

        // Only hold the lock briefly to check/update state — never wait inside the lock
        autoStartMutex.withLock {
            val existing = autoStartDeferred
            if (existing != null && existing.isActive) {
                shouldStart = false
                deferred = existing
            } else {
                shouldStart = true
                deferred = CompletableDeferred()
                autoStartDeferred = deferred
            }
        }

        if (!shouldStart) {
            // Another coroutine is starting the server; await its result without holding the lock
            logger.info("Another coroutine is already starting the server, waiting...")
            try {
                val success = withTimeoutOrNull(30_000L) { deferred.await() } ?: false
                if (!success) {
                    throw NetworkException(
                        "Server auto-start failed or timed out. Please manually start it using 'preci server start'.",
                        exception
                    )
                }
            } catch (e: NetworkException) {
                throw e
            } catch (e: Exception) {
                throw NetworkException(
                    "Error waiting for server auto-start: ${e.message}",
                    exception
                )
            }

            logger.info("Server auto-start completed by another coroutine, retrying request")
            return@withContext when {
                isPost && body != null -> post(path, body)
                isDelete -> delete(path)
                else -> get(path)
            }
        }

        // This coroutine is responsible for starting the server
        try {
            val project = com.intellij.openapi.project.ProjectManager.getInstance().defaultProject
            val serverManagementService = com.codecc.preci.service.server.ServerManagementService.getInstance(project)

            val startResult = serverManagementService.startServer()

            when (startResult) {
                is com.codecc.preci.service.server.ServerStartResult.Success -> {
                    logger.info("PreCI Local Server auto-started successfully, retrying request")
                    deferred.complete(true)
                    when {
                        isPost && body != null -> post(path, body)
                        isDelete -> delete(path)
                        else -> get(path)
                    }
                }
                is com.codecc.preci.service.server.ServerStartResult.Failure -> {
                    logger.error("Failed to auto-start PreCI Local Server: ${startResult.message}")
                    deferred.complete(false)
                    throw NetworkException(
                        "PreCI Server is not running and auto-start failed: ${startResult.message}. " +
                            "Please manually start it using 'preci server start'.",
                        exception
                    )
                }
            }
        } catch (e: Exception) {
            if (!deferred.isCompleted) {
                deferred.complete(false)
            }
            if (e is NetworkException) throw e
            throw NetworkException("Failed to auto-start server: ${e.message}", exception)
        }
    }

    /**
     * 执行 HTTP 请求
     *
     * @param request OkHttp 请求对象
     * @param client 自定义 OkHttpClient，为 null 时使用默认 httpClient
     * @return 响应体字符串
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常（4xx/5xx）
     */
    private fun executeRequest(request: Request, client: OkHttpClient? = null): String {
        try {
            val response = (client ?: httpClient).newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                var errorMessage = responseBody.ifEmpty { "HTTP ${response.code}" }
                var errorCode: Int? = null

                try {
                    val errorResponse = json.decodeFromString<ErrorResponse>(responseBody)
                    errorMessage = errorResponse.error
                    errorCode = errorResponse.extractErrorCode()

                    logger.warn("API 调用失败 - HTTP ${response.code}, 错误码: $errorCode, 错误信息: ${errorResponse.extractErrorMessage()}")
                } catch (e: Exception) {
                    logger.warn("无法解析错误响应: $responseBody")
                }

                throw BusinessException(response.code, errorMessage, errorCode)
            }

            return responseBody

        } catch (e: BusinessException) {
            throw e
        } catch (e: IOException) {
            if (isReadTimeout(e)) {
                logger.warn("Server response timeout during API call: ${request.url}", e)
                throw ServerBusyException(
                    "PreCI Local Server 响应超时，服务可能正忙，请稍后重试", e
                )
            }

            logger.warn("Network error during API call", e)
            throw NetworkException("Network error: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Unexpected error during API call", e)
            throw NetworkException("Unexpected error: ${e.message}", e)
        }
    }

    /**
     * 判断是否是读取超时（服务可达但未在超时时间内响应）
     */
    private fun isReadTimeout(exception: IOException): Boolean {
        return exception is SocketTimeoutException &&
               exception.message?.lowercase()?.contains("read") == true
    }

    /**
     * 反序列化 JSON 响应
     *
     * @param T 目标类型
     * @param json JSON 字符串
     * @return 反序列化后的对象
     * @throws SerializationException 序列化异常
     */
    private inline fun <reified T> decodeResponse(json: String): T {
        return try {
            this.json.decodeFromString<T>(json)
        } catch (e: KSerializationException) {
            logger.error("Failed to deserialize response: $json", e)
            throw com.codecc.preci.core.http.SerializationException(
                "Failed to deserialize response: ${e.message}",
                e
            )
        } catch (e: Exception) {
            logger.error("Unexpected error during deserialization: $json", e)
            throw com.codecc.preci.core.http.SerializationException(
                "Unexpected deserialization error: ${e.message}",
                e
            )
        }
    }

    /**
     * 序列化请求对象为 JSON
     *
     * @param T 请求对象类型
     * @param request 请求对象
     * @return JSON 字符串
     * @throws SerializationException 序列化异常
     */
    private inline fun <reified T> encodeRequest(request: T): String {
        return try {
            json.encodeToString(kotlinx.serialization.serializer(), request)
        } catch (e: KSerializationException) {
            logger.error("Failed to serialize request: $request", e)
            throw com.codecc.preci.core.http.SerializationException(
                "Failed to serialize request: ${e.message}",
                e
            )
        } catch (e: Exception) {
            logger.error("Unexpected error during serialization: $request", e)
            throw com.codecc.preci.core.http.SerializationException(
                "Unexpected serialization error: ${e.message}",
                e
            )
        }
    }

    // ========== API 接口方法 ==========

    /**
     * 1. 用户登录认证
     *
     * 支持快速登录和手动登录两种方式
     *
     * @param request 登录请求
     * @return 登录响应，包含项目 ID 和用户 ID
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常（如认证失败）
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun login(request: LoginRequest): LoginResponse {
        val requestBody = encodeRequest(request)
        val responseBody = post("/auth/login", requestBody)
        return decodeResponse(responseBody)
    }

    /**
     * 2. 项目初始化
     *
     * 初始化项目扫描配置，准备代码扫描所需的配置和环境
     *
     * @param request 初始化请求
     * @return 初始化响应，包含项目根目录路径
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun initProject(request: InitRequest): InitResponse {
        val requestBody = encodeRequest(request)
        val responseBody = post("/task/init", requestBody)
        logger.info("initProject raw response: $responseBody")
        return decodeResponse(responseBody)
    }

    /**
     * 2.1 重载/下载工具
     *
     * 在 `/task/init` 返回工具列表后，逐个调用此接口下载对应工具。
     * 将原来 init 中的工具下载拆分出来，避免单个请求耗时过长。
     *
     * @param toolName 工具名称（来自 [InitResponse.tools]）
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     *
     * @since 1.0
     */
    suspend fun reloadTool(toolName: String) {
        get("/task/reload/tool/$toolName")
    }

    /**
     * 3. 执行代码扫描
     *
     * 支持全量扫描、目标扫描和增量扫描（pre-commit/pre-push）
     *
     * @param request 扫描请求
     * @return 扫描响应，包含扫描启动信息、使用的工具列表和扫描文件数量
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun scan(request: ScanRequest): ScanResponse {
        val requestBody = encodeRequest(request)
        val responseBody = post("/task/scan", requestBody)
        return decodeResponse(responseBody)
    }

    /**
     * 4. 查询扫描进度
     *
     * 获取当前扫描任务的进度状态
     *
     * @return 扫描进度响应，包含项目根目录、各工具状态和整体状态
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun getScanProgress(): ScanProgressResponse {
        val responseBody = get("/task/scan/progress")
        return decodeResponse(responseBody)
    }

    /**
     * 5. 查询扫描结果
     *
     * 获取扫描结果（代码缺陷列表）
     *
     * @param request 扫描结果查询请求
     * @return 扫描结果响应，包含缺陷列表
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun getScanResult(request: ScanResultRequest): ScanResultResponse {
        val requestBody = encodeRequest(request)
        val responseBody = post("/task/scan/result", requestBody)
        return decodeResponse(responseBody)
    }

    /**
     * 6. 取消扫描
     *
     * 取消当前正在进行的扫描任务
     *
     * @return 取消扫描响应，包含被取消扫描的项目根目录
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun cancelScan(): ScanCancelResponse {
        val responseBody = get("/task/scan/cancel")
        return decodeResponse(responseBody)
    }

    /**
     * 7. 获取规则集列表
     *
     * 获取所有可用的代码检查规则集
     *
     * @return 规则集列表响应
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun getCheckerSetList(): CheckerSetListResponse {
        val responseBody = get("/checker/set/list")
        logger.info("getCheckerSetList: $responseBody")
        return decodeResponse(responseBody)
    }

    /**
     * 8. 选择规则集
     *
     * 选择要使用的代码检查规则集
     *
     * @param request 规则集选择请求
     * @return 规则集选择响应，包含项目根目录和已选择的规则集 ID 列表
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun selectCheckerSet(request: CheckerSetSelectRequest): CheckerSetSelectResponse {
        val requestBody = encodeRequest(request)
        val responseBody = post("/checker/set/select", requestBody)
        return decodeResponse(responseBody)
    }

    /**
     * 8.1 取消选择规则集
     *
     * 取消已选择的代码检查规则集，服务端会删除对应的配置文件
     *
     * @param request 规则集取消选择请求（复用 [CheckerSetSelectRequest]）
     * @return 规则集取消选择响应，包含项目根目录和剩余已选择的规则集 ID 列表
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun unselectCheckerSet(request: CheckerSetSelectRequest): CheckerSetSelectResponse {
        val requestBody = encodeRequest(request)
        val responseBody = post("/checker/set/unselect", requestBody)
        return decodeResponse(responseBody)
    }

    /**
     * 9. 停止 Local Server
     *
     * 停止 PreCI Local Server
     *
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     *
     * @since 1.0
     */
    suspend fun shutdown() {
        get("/shutdown")
    }

    /**
     * 10. 获取最新版本
     *
     * 获取线上最新版本信息
     *
     * @return 最新版本响应
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun getLatestVersion(): LatestVersionResponse {
        val responseBody = get("/misc/latestVersion")
        return decodeResponse(responseBody)
    }

    /**
     * 11. 下载最新版本
     *
     * 下载并安装最新版本的 PreCI
     *
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     *
     * @since 1.0
     */
    suspend fun downloadLatest() {
        get("/misc/downloadLatest")
    }

    /**
     * 12. 获取项目列表
     *
     * 获取当前用户有权限的蓝盾项目列表
     *
     * @return 项目列表响应，包含所有可用的项目信息
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常（如未登录）
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun getProjects(): ProjectListResponse {
        val responseBody = get("/auth/list/projects")
        return decodeResponse(responseBody)
    }

    /**
     * 13. 设置当前项目
     *
     * 设置当前使用的蓝盾项目
     *
     * @param projectId 要设置的项目 ID
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常（如项目不存在或无权限）
     * @throws ServerNotRunningException Server 未运行
     *
     * @since 1.0
     */
    suspend fun setProject(projectId: String) {
        get("/auth/project/$projectId")
    }

    /**
     * 14. 获取当前绑定的项目
     *
     * 获取当前用户绑定的蓝盾项目 ID
     *
     * @return 当前绑定项目响应，包含项目 ID（如果未绑定则为空字符串）
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun getCurrentProject(): GetProjectResponse {
        val responseBody = get("/auth/project")
        return decodeResponse(responseBody)
    }

    /**
     * 15. 获取远程任务列表
     *
     * 获取 CodeCC 平台的任务列表，通过 Local Server 代理访问。
     * 此接口经过 OauthTokenMiddleware，OAuth Token 由 Local Server 自动管理。
     *
     * @return 远程任务列表响应，包含任务 ID、英文名称和中文名称
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常（如 OAuth 认证失败）
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun getRemoteTaskList(): RemoteTaskListResponse {
        val responseBody = get("/codecc/task/list")
        return decodeResponse(responseBody)
    }

    /**
     * 16. 获取远程缺陷列表
     *
     * 获取 CodeCC 平台的缺陷列表，支持分页查询和多维度过滤。
     * 此接口经过 OauthTokenMiddleware，OAuth Token 由 Local Server 自动管理。
     *
     * @param request 缺陷列表查询请求，包含过滤条件和分页参数
     * @return 远程缺陷列表响应，包含缺陷列表和统计信息
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常（如 OAuth 认证失败）
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun getRemoteDefectList(request: RemoteDefectListRequest): RemoteDefectListResponse {
        val requestBody = encodeRequest(request)
        val responseBody = post("/codecc/defect/list", requestBody)
        return decodeResponse(responseBody)
    }

    // ========== PAC 流水线 API 接口 ==========

    /**
     * 17. 获取流水线构建历史
     *
     * 获取当前项目的流水线构建历史记录。
     *
     * @param rootPath 项目根目录路径
     * @param page 页码，默认 1
     * @param pageSize 每页数量，默认 20
     * @return 构建历史响应
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun getPipelineBuildHistory(
        rootPath: String,
        page: Int = 1,
        pageSize: Int = 20
    ): PipelineBuildHistoryResponse {
        val encodedPath = java.net.URLEncoder.encode(rootPath, "UTF-8")
        val responseBody = get("/pipeline/build/history?rootPath=$encodedPath&page=$page&pageSize=$pageSize")
        return decodeResponse(responseBody)
    }

    /**
     * 18. 触发流水线构建
     *
     * 触发一次新的流水线构建。
     *
     * @param request 构建请求，包含项目根目录路径
     * @return 触发构建响应，包含新构建的 buildId
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun startPipelineBuild(request: StartBuildRequest): StartBuildResponse {
        val requestBody = encodeRequest(request)
        val responseBody = post("/pipeline/build/start", requestBody, readTimeoutSeconds = BUILD_START_TIMEOUT_SECONDS)
        return decodeResponse(responseBody)
    }

    companion object {
        /** 触发构建接口超时（秒），该接口可能需要安装 Agent 等耗时准备工作 */
        const val BUILD_START_TIMEOUT_SECONDS = 600L
    }

    /**
     * 19. 获取流水线构建日志
     *
     * 获取指定构建的日志内容，支持增量获取。
     *
     * @param rootPath 项目根目录路径
     * @param buildId 构建唯一标识 ID
     * @param start 起始行号，默认为 0
     * @return 构建日志响应
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun getPipelineBuildLogs(
        rootPath: String,
        buildId: String,
        start: Long = 0
    ): PipelineBuildLogsResponse {
        val encodedPath = java.net.URLEncoder.encode(rootPath, "UTF-8")
        val responseBody = get("/pipeline/build/$buildId/logs?rootPath=$encodedPath&start=$start")
        return decodeResponse(responseBody)
    }

    /**
     * 20. 获取流水线构建详情
     *
     * 获取指定构建的详细信息。
     *
     * @param rootPath 项目根目录路径
     * @param buildId 构建唯一标识 ID
     * @return 构建详情响应
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 1.0
     */
    suspend fun getPipelineBuildDetail(
        rootPath: String,
        buildId: String
    ): PipelineBuildDetailResponse {
        val encodedPath = java.net.URLEncoder.encode(rootPath, "UTF-8")
        val responseBody = get("/pipeline/build/$buildId/detail?rootPath=$encodedPath")
        return decodeResponse(responseBody)
    }

    /**
     * 21. 停止流水线构建
     *
     * 停止正在运行的流水线构建。
     *
     * @param rootPath 项目根目录路径
     * @param buildId 构建唯一标识 ID
     * @return 是否成功停止（未抛异常即为成功）
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     *
     * @since 1.0
     */
    suspend fun stopPipelineBuild(rootPath: String, buildId: String): Boolean {
        val encodedPath = java.net.URLEncoder.encode(rootPath, "UTF-8")
        delete("/pipeline/build/$buildId/stop?rootPath=$encodedPath")
        return true
    }

    /**
     * 22. OAuth Device Login
     *
     * 将插件获取的 OAuth token 传给 Local Server 存储和管理。
     * Local Server 会验证 token 有效性，提取用户信息，并负责后续的 token 刷新。
     *
     * @param request OAuth token 信息
     * @return 登录响应，包含用户 ID 和项目 ID
     * @throws NetworkException 网络异常
     * @throws BusinessException 业务异常
     * @throws ServerNotRunningException Server 未运行
     * @throws SerializationException 序列化/反序列化异常
     *
     * @since 2.0
     */
    suspend fun oauthDeviceLogin(request: OAuthDeviceLoginRequest): LoginResponse {
        val requestBody = encodeRequest(request)
        val responseBody = post("/auth/oauth/device/login", requestBody)
        return decodeResponse(responseBody)
    }

}

