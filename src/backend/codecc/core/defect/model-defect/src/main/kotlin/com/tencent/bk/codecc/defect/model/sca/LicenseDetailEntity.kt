package com.tencent.bk.codecc.defect.model.sca

import com.tencent.codecc.common.db.CommonEntity
import com.tencent.devops.common.constant.ComConstants.Status
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

/**
 * 证书详情信息
 */
@CompoundIndexes(
    CompoundIndex(name = "idx_name_1", def = "{'name':1}", background = true),
    CompoundIndex(name = "idx_alias_1", def = "{'alias':1}", background = true)
)
@Document(collection = "t_license_detail")
class LicenseDetailEntity(

    /**
     * 证书名称
     */
    @Field("name")
    var name: String = "",
    /**
     * 证书名称(全名)
     */
    @Field("full_name")
    var fullName: String = "",
    /**
     * 别名
     */
    @Field("alias")
    var alias: List<String> = emptyList(),
    /**
     * 严重程度
     */
    @Field("severity")
    var severity: Int = 0,
    /**
     * 风险说明
     */
    @Field("severity_desc")
    var severityDesc: String? = null,
    /**
     * GPL兼容
     */
    @Field("gpl_compatible")
    var gplCompatible: Boolean? = null,
    /**
     * GPL说明
     */
    @Field("gpl_desc")
    var gplDesc: String? = null,
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
     * 许可证链接
     */
    @Field("urls")
    var urls: List<String> = emptyList(),
    /**
     * 摘要
     */
    @Field("summary")
    var summary: String? = null,
    /**
     * 状态
     */
    @Field("status")
    var status: Int = Status.ENABLE.value(),
    /**
     * 证书授予的权利
     */
    @Field("permitted")
    var permitted: List<String> = emptyList(),
    /**
     * 证书禁止的权利
     */
    @Field("forbidden")
    var forbidden: List<String> = emptyList(),

    /**
     * 证书必须的义务
     */
    @Field("required")
    var required: List<String> = emptyList(),

    /**
     * 证书无需的义务
     */
    @Field("unnecessary")
    var unnecessary: List<String> = emptyList(),
) : CommonEntity()
