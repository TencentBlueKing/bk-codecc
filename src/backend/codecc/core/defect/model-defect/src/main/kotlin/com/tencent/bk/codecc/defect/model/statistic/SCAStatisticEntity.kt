package com.tencent.bk.codecc.defect.model.statistic

import lombok.Data
import lombok.EqualsAndHashCode
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.Sharded

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_sca_statistic")
@CompoundIndexes(
    CompoundIndex(
        name = "idx_task_id_1_tool_name_1_build_id_1",
        def = "{'task_id': 1, 'tool_name': 1, 'build_id': 1}",
        background = true
    ),
    CompoundIndex(name = "idx_task_id_1_build_id_1", def = "{'task_id': 1, 'build_id': 1}", background = true),
    CompoundIndex(
        name = "idx_task_id_1_tool_name_1_time_1",
        def = "{'task_id': 1, 'tool_name': 1, 'time': 1}",
        background = true
    )
)
@Sharded(shardKey = ["task_id"])
class SCAStatisticEntity(
    /**
     * 组件数量
     */
    @Field("package_count")
    var packageCount: Long = 0,
    /**
     * 高风险组件数量
     */
    @Field("high_package_count")
    var highPackageCount: Long = 0,
    /**
     * 中风险组件数量
     */
    @Field("medium_package_count")
    var mediumPackageCount: Long = 0,
    /**
     * 低风险组件数量
     */
    @Field("low_package_count")
    var lowPackageCount: Long = 0,
    /**
     * 未知风险组件数量
     */
    @Field("unknown_package_count")
    var unknownPackageCount: Long = 0,
    /**
     * 漏洞数量
     */
    @Field("new_vul_count")
    var newVulCount: Long = 0,
    /**
     * 高风险漏洞数量
     */
    @Field("new_high_vul_count")
    var newHighVulCount: Long = 0,
    /**
     * 中风险漏洞数量
     */
    @Field("new_medium_vul_count")
    var newMediumVulCount: Long = 0,
    /**
     * 低风险漏洞数量
     */
    @Field("new_low_vul_count")
    var newLowVulCount: Long = 0,
    /**
     * 未知风险漏洞数量
     */
    @Field("new_unknown_vul_count")
    var newUnknownVulCount: Long = 0,
    /**
     * 证书数量
     */
    @Field("license_count")
    var licenseCount: Long = 0,
    /**
     * 高风险证书数量
     */
    @Field("high_license_count")
    var highLicenseCount: Long = 0,
    /**
     * 中风险证书数量
     */
    @Field("medium_license_count")
    var mediumLicenseCount: Long = 0,
    /**
     * 低风险证书数量
     */
    @Field("low_license_count")
    var lowLicenseCount: Long = 0,
    /**
     * 未知风险证书数量
     */
    @Field("unknown_license_count")
    var unknownLicenseCount: Long = 0,
) : StatisticEntity()
