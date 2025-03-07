package com.tencent.bk.codecc.defect.model.sca

import com.tencent.codecc.common.db.CommonEntity
import com.tencent.devops.common.constant.ComConstants.DefectStatus
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.Sharded

/**
 * SBOM 组件生成记录
 */
@CompoundIndexes(
    CompoundIndex(name = "idx_task_id_1_tool_name_1_name_1", def = "{'task_id':1, 'tool_name':1, 'name':1}",
        background = true),
    CompoundIndex(name = "idx_task_id_1_tool_name_1_status_1", def = "{'task_id':1, 'tool_name':1, 'status':1}",
        background = true),
)
@Sharded(shardKey = ["task_id"])
@Document(collection = "t_sca_license")
class SCALicenseEntity(
    /**
     * 任务ID
     */
    @Field("task_id")
    var taskId: Long = 0L,
    /**
     * 工具名称
     */
    @Field("tool_name")
    var toolName: String = "",
    /**
     * 证书名称
     */
    @Field("name")
    var name: String = "",
    /**
     * 证书全名
     */
    @Field("full_name")
    var fullName: String = "",
    /**
     * 状态
     */
    @Field("status")
    var status: Int = DefectStatus.NEW.value(),
    /**
     * 严重程度
     */
    @Field("severity")
    var severity: Int = 0,
    /**
     * GPL兼容
     */
    @Field("gpl_compatible")
    var gplCompatible: Boolean? = null,
    /**
     * 是否是osi认证
     */
    @Field("osi")
    var osi: Boolean = false,
    /**
     * 是否是FSF认证
     */
    @Field("fsf")
    var fsf: Boolean = false,
    /**
     * 是否是SPDX认证
     */
    @Field("spdx")
    var spdx: Boolean = false,
    /**
     * 状态
     */
    @Field("has_enabled_package")
    var hasEnabledPackage: Boolean = true,
    /**
     * 告警忽略时间
     */
    @Field("ignore_time")
    var ignoreTime: Long? = null,

    /**
     * 告警忽略原因类型
     */
    @Field("ignore_reason_type")
    var ignoreReasonType: Int? = null,

    /**
     * 告警忽略原因
     */
    @Field("ignore_reason")
    var ignoreReason: String? = null,
    /**
     * 告警忽略操作人
     */
    @Field("ignore_author")
    var ignoreAuthor: String? = null,

    /**
     * 修复时的构建号
     */
    @Field("fixed_build_number")
    var fixedBuildNumber: String? = null,
    /**
     * 告警修复时间
     */
    @Field("fixed_time")
    var fixedTime: Long? = null,

    ) : CommonEntity() {
    constructor(taskId: Long, toolName: String, name: String, status: Int, hasEnabledPackage: Boolean) : this(
        taskId, toolName, name, "", status, 0, false, false, false, false, hasEnabledPackage
    )
}
