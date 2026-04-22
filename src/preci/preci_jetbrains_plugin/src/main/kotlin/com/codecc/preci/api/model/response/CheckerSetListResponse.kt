package com.codecc.preci.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 规则集信息
 *
 * @property checkerSetId 规则集唯一标识 ID
 * @property checkerSetName 规则集显示名称
 * @property toolName 规则集对应的检查工具（如 golangci-lint）
 *
 * @since 1.0
 */
@Serializable
data class CheckerSet(
    @SerialName("checkerSetId")
    val checkerSetId: String,
    
    @SerialName("checkerSetName")
    val checkerSetName: String,
    
    @SerialName("toolName")
    val toolName: String
)

/**
 * 规则集列表响应
 *
 * @property checkerSets 规则集列表
 *
 * @since 1.0
 */
@Serializable
data class CheckerSetListResponse(
    @SerialName("checkerSets")
    val checkerSets: List<CheckerSet>
)

