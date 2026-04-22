package com.codecc.preci.service.codecc

import com.codecc.preci.api.model.response.RemoteDefectListResponse
import com.codecc.preci.api.model.response.RemoteTaskInfo
import com.codecc.preci.api.model.response.RemoteTaskListResponse

/**
 * 远程任务列表查询结果密封类
 *
 * 表示获取 CodeCC 平台任务列表操作的结果，使用密封类保证类型安全。
 *
 * @since 1.0
 */
sealed class RemoteTaskListResult {
    /**
     * 查询成功
     *
     * @property tasks 任务信息列表
     * @property response 原始 API 响应
     */
    data class Success(
        val tasks: List<RemoteTaskInfo>,
        val response: RemoteTaskListResponse
    ) : RemoteTaskListResult()

    /**
     * 查询失败
     *
     * @property message 失败原因描述
     * @property cause 原始异常（如果有）
     */
    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : RemoteTaskListResult()
}

/**
 * 远程缺陷列表查询结果密封类
 *
 * 表示获取 CodeCC 平台缺陷列表操作的结果，使用密封类保证类型安全。
 *
 * @since 1.0
 */
sealed class RemoteDefectListResult {
    /**
     * 查询成功
     *
     * @property response 原始 API 响应，包含缺陷列表和统计信息
     */
    data class Success(
        val response: RemoteDefectListResponse
    ) : RemoteDefectListResult()

    /**
     * 查询失败
     *
     * @property message 失败原因描述
     * @property cause 原始异常（如果有）
     */
    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : RemoteDefectListResult()
}
