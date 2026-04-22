package com.codecc.preci.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 流水线构建信息
 *
 * 表示一次流水线构建的基本信息，由 `GET /pipeline/build/history` 接口返回。
 *
 * @property buildId 构建唯一标识 ID
 * @property buildNum 构建序号
 * @property startTime 构建开始时间（Unix 时间戳，毫秒）
 * @property endTime 构建结束时间（Unix 时间戳，毫秒，未结束时为 0）
 * @property status 构建状态（SUCCEED / FAILED / RUNNING / CANCELED / QUEUE）
 *
 * @since 1.0
 */
@Serializable
data class PipelineBuild(
    @SerialName("buildId")
    val buildId: String = "",

    @SerialName("buildNum")
    val buildNum: Long = 0,

    @SerialName("startTime")
    val startTime: Long = 0,

    @SerialName("endTime")
    val endTime: Long = 0,

    @SerialName("status")
    val status: String = ""
)

/**
 * 流水线构建历史响应
 *
 * `GET /pipeline/build/history` 接口的响应体，包含分页的构建记录列表。
 *
 * @property builds 构建记录列表
 *
 * @since 1.0
 */
@Serializable
data class PipelineBuildHistoryResponse(
    @SerialName("builds")
    val builds: List<PipelineBuild>? = null
) {
    /** 获取非空构建列表 */
    fun buildsOrEmpty(): List<PipelineBuild> = builds ?: emptyList()
}

/**
 * 触发构建响应
 *
 * `POST /pipeline/build/start` 接口的响应体，包含新触发的构建 ID。
 *
 * @property buildId 新构建的唯一标识 ID
 *
 * @since 1.0
 */
@Serializable
data class StartBuildResponse(
    @SerialName("buildId")
    val buildId: String = ""
)

/**
 * 流水线构建日志行
 *
 * 表示构建日志的单行内容。
 *
 * @property lineNo 日志行号
 * @property message 日志消息内容
 * @property timestamp 日志时间戳（Unix 时间戳，毫秒）
 *
 * @since 1.0
 */
@Serializable
data class PipelineBuildLog(
    @SerialName("lineNo")
    val lineNo: Long = 0,

    @SerialName("message")
    val message: String = "",

    @SerialName("timestamp")
    val timestamp: Long = 0
)

/**
 * 流水线构建日志响应
 *
 * `GET /pipeline/build/{buildId}/logs` 接口的响应体，包含构建日志内容。
 *
 * @property buildId 构建唯一标识 ID
 * @property finished 日志是否已全部输出完毕
 * @property logs 日志行列表
 *
 * @since 1.0
 */
@Serializable
data class PipelineBuildLogsResponse(
    @SerialName("buildId")
    val buildId: String = "",

    @SerialName("finished")
    val finished: Boolean = false,

    @SerialName("logs")
    val logs: List<PipelineBuildLog>? = null
) {
    /** 获取非空日志列表 */
    fun logsOrEmpty(): List<PipelineBuildLog> = logs ?: emptyList()
}

/**
 * 流水线构建详情响应
 *
 * `GET /pipeline/build/{buildId}/detail` 接口的响应体，包含构建详细信息。
 *
 * @property id 构建唯一标识 ID
 * @property status 构建状态
 * @property startTime 构建开始时间（Unix 时间戳，毫秒）
 * @property endTime 构建结束时间（Unix 时间戳，毫秒）
 * @property buildNum 构建序号
 *
 * @since 1.0
 */
@Serializable
data class PipelineBuildDetailResponse(
    @SerialName("id")
    val id: String = "",

    @SerialName("status")
    val status: String = "",

    @SerialName("startTime")
    val startTime: Long = 0,

    @SerialName("endTime")
    val endTime: Long = 0,

    @SerialName("buildNum")
    val buildNum: Long = 0
)
