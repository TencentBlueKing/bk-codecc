package com.codecc.preci.api.client

import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.server.ServerManagementService
import com.codecc.preci.service.server.ServerStartResult
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * 服务自动启动拦截器
 *
 * 检测网络连接失败错误，自动启动 PreCI Local Server 并重试请求。
 *
 * **工作流程：**
 * 1. 拦截网络异常（ConnectException、SocketTimeoutException）
 * 2. 判断是否是连接失败错误
 * 3. 如果是，自动调用 `preci server start` 启动服务
 * 4. 启动成功后，用新端口重建请求 URL 并重试
 * 5. 如果启动失败或重试仍失败，抛出原始异常
 *
 * **防止死循环：**
 * - 每个请求最多自动启动 1 次
 * - 启动失败后不再重试
 * - 使用标志位防止并发启动
 *
 * **典型场景：**
 * - 用户关闭了 PreCI Server
 * - 用户重启了系统，Server 未自动启动
 * - Server 崩溃或被意外终止
 * - Server 更新后端口发生变化
 *
 * **优势：**
 * - 对上层服务透明，自动恢复服务连接
 * - 避免用户手动启动服务
 * - 提升用户体验
 *
 * @since 1.0
 */
class ServerAutoStartInterceptor : Interceptor {
    private val logger = PreCILogger.getLogger(ServerAutoStartInterceptor::class.java)

    /**
     * 标记是否正在自动启动服务（防止并发启动）
     */
    @Volatile
    private var isAutoStartInProgress = false

    /**
     * 记录已尝试自动启动的请求（防止同一请求多次启动）
     * 使用 ThreadLocal 确保线程安全
     */
    private val autoStartAttempted = ThreadLocal.withInitial { mutableSetOf<String>() }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestKey = "${request.method}:${request.url}"

        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (shouldAutoStartServer(e, requestKey)) {
                logger.info("检测到连接失败，尝试自动启动 PreCI Server")

                val newPort = performAutoStart()

                if (newPort != null) {
                    logger.info("PreCI Server 启动成功，使用新端口 $newPort 重试请求")
                    try {
                        val newUrl = request.url.newBuilder()
                            .port(newPort)
                            .build()
                        val newRequest = request.newBuilder()
                            .url(newUrl)
                            .build()
                        return chain.proceed(newRequest)
                    } catch (retryException: IOException) {
                        logger.warn("重试请求仍然失败", retryException)
                        throw retryException
                    }
                } else {
                    logger.warn("PreCI Server 自动启动失败，返回原始错误")
                    throw e
                }
            } else {
                throw e
            }
        } finally {
            autoStartAttempted.get()?.remove(requestKey)
        }
    }

    /**
     * 判断是否应该自动启动服务
     *
     * @param exception IO 异常
     * @param requestKey 请求唯一标识
     * @return true 如果应该启动服务，false 否则
     */
    private fun shouldAutoStartServer(exception: IOException, requestKey: String): Boolean {
        if (isAutoStartInProgress) {
            logger.debug("服务正在启动中，跳过自动启动")
            return false
        }

        if (autoStartAttempted.get()?.contains(requestKey) == true) {
            logger.debug("该请求已尝试过自动启动，跳过")
            return false
        }

        if (!isConnectionFailureError(exception)) {
            logger.debug("不是连接失败错误，跳过自动启动")
            return false
        }

        autoStartAttempted.get()?.add(requestKey)

        return true
    }

    /**
     * 判断是否是连接失败错误
     *
     * 仅识别 **TCP 连接本身失败** 的场景，不包括 Read timed out（服务可达但繁忙）。
     * - ConnectException: 连接被拒绝 / 无法建立连接
     * - SocketTimeoutException 且消息含 "connect": 连接超时（与 "Read timed out" 区分）
     *
     * @param exception IO 异常
     * @return true 如果是连接阶段失败，false 否则（包括 Read timed out）
     */
    private fun isConnectionFailureError(exception: IOException): Boolean {
        if (exception is ConnectException) return true

        if (exception is SocketTimeoutException) {
            val msg = exception.message?.lowercase() ?: ""
            return msg.contains("connect") && !msg.contains("read")
        }

        val message = exception.message?.lowercase() ?: ""
        val causeMessage = exception.cause?.message?.lowercase() ?: ""

        return message.contains("failed to connect") ||
                message.contains("connection refused") ||
                causeMessage.contains("failed to connect") ||
                causeMessage.contains("connection refused")
    }

    /**
     * 执行自动启动服务
     *
     * @return 启动成功时返回新端口号，失败返回 null
     */
    private fun performAutoStart(): Int? {
        return try {
            isAutoStartInProgress = true

            logger.info("开始自动启动 PreCI Server")

            val project = try {
                ProjectManager.getInstance().defaultProject
            } catch (e: Exception) {
                logger.error("无法获取项目实例", e)
                return null
            }

            val serverService = try {
                ServerManagementService.getInstance(project)
            } catch (e: Exception) {
                logger.error("无法获取 ServerManagementService", e)
                return null
            }

            val result = runBlocking {
                serverService.startServer()
            }

            when (result) {
                is ServerStartResult.Success -> {
                    logger.info("PreCI Server 自动启动成功，端口: ${result.port}")
                    result.port
                }
                is ServerStartResult.Failure -> {
                    logger.warn("PreCI Server 自动启动失败: ${result.message}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("自动启动服务时发生异常", e)
            null
        } finally {
            isAutoStartInProgress = false
        }
    }
}

