package com.codecc.preci.api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 远程缺陷列表查询请求
 *
 * `POST /codecc/defect/list` 接口的请求体，支持分页查询和多维度过滤。
 * 通过 Local Server 代理访问 CodeCC 后端，需要 OAuth 认证。
 *
 * @property projectRoot 项目根目录路径，用于将文件相对路径转换为本地绝对路径（必填）
 * @property taskIdList 过滤指定任务 ID 的缺陷
 * @property toolNameList 过滤指定工具产生的缺陷
 * @property dimensionList 过滤指定维度的缺陷
 * @property checker 过滤指定检查规则的缺陷
 * @property author 过滤指定责任人的缺陷
 * @property severity 严重级别过滤，默认 ["1","2","4","8"]（全部级别）
 * @property status 缺陷状态过滤，默认 ["1"]（待修复）
 * @property fileList 过滤指定文件的缺陷
 * @property defectType 过滤指定类型的缺陷
 * @property buildId 过滤指定构建 ID 的缺陷
 * @property pageNum 页码，默认 1
 * @property pageSize 每页数量，默认 100
 * @property sortField 排序字段，默认 "fileName"
 * @property sortType 排序方式，"ASC" 升序 / "DESC" 降序，默认 "ASC"
 *
 * @since 1.0
 */
@Serializable
data class RemoteDefectListRequest(
    @SerialName("projectRoot")
    val projectRoot: String,

    @SerialName("taskIdList")
    val taskIdList: List<Long>? = null,

    @SerialName("toolNameList")
    val toolNameList: List<String>? = null,

    @SerialName("dimensionList")
    val dimensionList: List<String>? = null,

    @SerialName("checker")
    val checker: String? = null,

    @SerialName("author")
    val author: String? = null,

    @SerialName("severity")
    val severity: List<String>? = null,

    @SerialName("status")
    val status: List<String>? = null,

    @SerialName("fileList")
    val fileList: List<String>? = null,

    @SerialName("defectType")
    val defectType: List<String>? = null,

    @SerialName("buildId")
    val buildId: String? = null,

    @SerialName("pageNum")
    val pageNum: Int? = null,

    @SerialName("pageSize")
    val pageSize: Int? = null,

    @SerialName("sortField")
    val sortField: String? = null,

    @SerialName("sortType")
    val sortType: String? = null
)
