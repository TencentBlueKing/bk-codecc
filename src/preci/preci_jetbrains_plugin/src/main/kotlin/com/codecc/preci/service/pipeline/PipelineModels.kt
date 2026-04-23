package com.codecc.preci.service.pipeline

import com.codecc.preci.api.model.response.PipelineBuild
import com.codecc.preci.api.model.response.PipelineBuildDetailResponse
import com.codecc.preci.api.model.response.PipelineBuildLog

/**
 * 构建历史查询结果密封类
 *
 * 表示获取流水线构建历史操作的结果，使用密封类保证类型安全。
 *
 * @since 1.0
 */
sealed class BuildHistoryResult {
    /**
     * 查询成功
     *
     * @property builds 构建记录列表
     */
    data class Success(val builds: List<PipelineBuild>) : BuildHistoryResult()

    /**
     * 查询失败
     *
     * @property message 失败原因描述
     * @property cause 原始异常（如果有）
     */
    data class Failure(val message: String, val cause: Throwable? = null) : BuildHistoryResult()
}

/**
 * 触发构建结果密封类
 *
 * 表示触发流水线构建操作的结果，使用密封类保证类型安全。
 *
 * @since 1.0
 */
sealed class StartBuildResult {
    /**
     * 触发成功
     *
     * @property buildId 新构建的唯一标识 ID
     */
    data class Success(val buildId: String) : StartBuildResult()

    /**
     * 触发失败
     *
     * @property message 失败原因描述
     * @property cause 原始异常（如果有）
     */
    data class Failure(val message: String, val cause: Throwable? = null) : StartBuildResult()
}

/**
 * 构建日志查询结果密封类
 *
 * 表示获取流水线构建日志操作的结果，使用密封类保证类型安全。
 *
 * @since 1.0
 */
sealed class BuildLogsResult {
    /**
     * 查询成功
     *
     * @property logs 日志行列表
     * @property finished 日志是否已全部输出完毕
     */
    data class Success(
        val logs: List<PipelineBuildLog>,
        val finished: Boolean
    ) : BuildLogsResult()

    /**
     * 查询失败
     *
     * @property message 失败原因描述
     * @property cause 原始异常（如果有）
     */
    data class Failure(val message: String, val cause: Throwable? = null) : BuildLogsResult()
}

/**
 * 构建详情查询结果密封类
 *
 * 表示获取流水线构建详情操作的结果，使用密封类保证类型安全。
 *
 * @since 1.0
 */
sealed class BuildDetailResult {
    /**
     * 查询成功
     *
     * @property detail 构建详情
     */
    data class Success(val detail: PipelineBuildDetailResponse) : BuildDetailResult()

    /**
     * 查询失败
     *
     * @property message 失败原因描述
     * @property cause 原始异常（如果有）
     */
    data class Failure(val message: String, val cause: Throwable? = null) : BuildDetailResult()
}

/**
 * 停止构建结果密封类
 *
 * 表示停止流水线构建操作的结果，使用密封类保证类型安全。
 *
 * @since 1.0
 */
sealed class StopBuildResult {
    /**
     * 停止成功
     */
    object Success : StopBuildResult()

    /**
     * 停止失败
     *
     * @property message 失败原因描述
     * @property cause 原始异常（如果有）
     */
    data class Failure(val message: String, val cause: Throwable? = null) : StopBuildResult()
}
