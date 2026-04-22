package com.codecc.preci.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 远程任务信息
 *
 * CodeCC 平台上的任务信息，由 `GET /codecc/task/list` 接口返回。
 *
 * @property taskId 任务唯一标识 ID
 * @property nameEn 任务英文名称
 * @property nameCn 任务中文名称
 *
 * @since 1.0
 */
@Serializable
data class RemoteTaskInfo(
    @SerialName("taskId")
    val taskId: Long,

    @SerialName("nameEn")
    val nameEn: String,

    @SerialName("nameCn")
    val nameCn: String
) {
    /**
     * 获取任务的显示名称
     *
     * 优先使用中文名称，若中文名称为空则回退到英文名称。
     *
     * @return 任务显示名称
     */
    fun getDisplayName(): String {
        return if (nameCn.isNotBlank()) nameCn else nameEn
    }

    /**
     * 重写 toString，确保可编辑 JComboBox 等 Swing 组件
     * 在任何场景下都只显示友好名称。
     */
    override fun toString(): String = getDisplayName()
}

/**
 * 远程任务列表响应
 *
 * `GET /codecc/task/list` 接口的响应体，包含 CodeCC 平台上的任务列表。
 *
 * @property taskInfos 任务列表
 *
 * @since 1.0
 */
@Serializable
data class RemoteTaskListResponse(
    @SerialName("taskInfos")
    val taskInfos: List<RemoteTaskInfo>? = null
) {
    /**
     * 获取任务列表（null 安全）
     *
     * @return 任务列表，若服务器返回 null 则返回空列表
     */
    fun getTaskList(): List<RemoteTaskInfo> = taskInfos ?: emptyList()
}
