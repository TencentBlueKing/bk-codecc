package com.codecc.preci.service.scan

import com.codecc.preci.api.model.response.InitResponse
import com.codecc.preci.api.model.response.ScanProgressResponse
import com.codecc.preci.api.model.response.ScanResponse
import com.codecc.preci.api.model.response.ScanResultResponse

/**
 * 扫描服务相关数据模型
 *
 * 封装扫描服务的各种操作结果，包括项目初始化、扫描执行等。
 *
 * @since 1.0
 */

/**
 * 初始化阶段
 */
enum class InitPhase {
    /** 正在调用 /task/init 接口 */
    INITIALIZING,
    /** 正在下载工具（调用 /task/reload/tool/{toolName}） */
    DOWNLOADING_TOOL,
    /** 全部完成 */
    COMPLETED
}

/**
 * 初始化进度信息，用于回调通知 UI 层当前进度
 *
 * @property phase 当前阶段
 * @property currentTool 当前正在下载的工具名（仅 DOWNLOADING_TOOL 阶段有值）
 * @property toolIndex 当前工具的索引（从 1 开始）
 * @property totalTools 需要下载的工具总数
 */
data class InitProgress(
    val phase: InitPhase,
    val currentTool: String? = null,
    val toolIndex: Int = 0,
    val totalTools: Int = 0
)

/**
 * 项目初始化结果
 *
 * 封装项目初始化操作的结果，包含成功和失败两种情况。
 *
 * @since 1.0
 */
sealed class InitResult {
    /**
     * 初始化成功
     *
     * @property response API 响应数据
     */
    data class Success(val response: InitResponse) : InitResult()

    /**
     * 初始化失败
     *
     * @property message 失败原因描述
     * @property exception 异常对象（可选）
     */
    data class Failure(
        val message: String,
        val exception: Exception? = null
    ) : InitResult()
}

/**
 * 扫描执行结果
 *
 * 封装代码扫描执行操作的结果（待实现）。
 *
 * @since 1.0
 */
sealed class ScanResult {
    /**
     * 扫描启动成功
     *
     * @property response API 响应数据
     */
    data class Success(val response: ScanResponse) : ScanResult()

    /**
     * 扫描启动失败
     *
     * @property message 失败原因描述
     * @property exception 异常对象（可选）
     */
    data class Failure(
        val message: String,
        val exception: Exception? = null
    ) : ScanResult()
}

/**
 * 扫描进度查询结果
 *
 * 封装扫描进度查询操作的结果（待实现）。
 *
 * @since 1.0
 */
sealed class ScanProgressResult {
    /**
     * 查询成功
     *
     * @property response API 响应数据
     */
    data class Success(val response: ScanProgressResponse) : ScanProgressResult()

    /**
     * 查询失败
     *
     * @property message 失败原因描述
     * @property exception 异常对象（可选）
     */
    data class Failure(
        val message: String,
        val exception: Exception? = null
    ) : ScanProgressResult()
}

/**
 * 扫描结果查询结果
 *
 * 封装扫描结果查询操作的结果（待实现）。
 *
 * @since 1.0
 */
sealed class ScanResultQueryResult {
    /**
     * 查询成功
     *
     * @property response API 响应数据
     */
    data class Success(val response: ScanResultResponse) : ScanResultQueryResult()

    /**
     * 查询失败
     *
     * @property message 失败原因描述
     * @property exception 异常对象（可选）
     */
    data class Failure(
        val message: String,
        val exception: Exception? = null
    ) : ScanResultQueryResult()
}

/**
 * 取消扫描结果
 *
 * 封装取消扫描操作的结果。
 *
 * @since 1.0
 */
sealed class CancelScanResult {
    /**
     * 取消成功
     *
     * @property projectRoot 被取消扫描的项目根目录
     */
    data class Success(val projectRoot: String) : CancelScanResult()

    /**
     * 取消失败
     *
     * @property message 失败原因描述
     * @property exception 异常对象（可选）
     */
    data class Failure(
        val message: String,
        val exception: Exception? = null
    ) : CancelScanResult()
}

