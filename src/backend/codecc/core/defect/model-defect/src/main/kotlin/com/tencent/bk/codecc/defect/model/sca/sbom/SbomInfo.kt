package com.tencent.bk.codecc.defect.model.sca.sbom

import com.tencent.codecc.common.db.CommonEntity
import org.springframework.data.mongodb.core.mapping.Field

/**
 * SBOM 生成信息
 */
open class SbomInfo(
    /**
     * SBOM ID
     */
    @Field("element_id")
    var elementId: String? = null,
    /**
     * 版本
     */
    @Field("version")
    var version: String? = null,
    /**
     * 创建信息
     */
    @Field("create_info")
    var createInfo: SbomCreationInfo? = null,
    /**
     * 名称
     */
    @Field("name")
    var name: String? = null,
    /**
     * 描述
     */
    @Field("comment")
    var comment: String? = null,
    /**
     * 数据许可证
     */
    @Field("data_license")
    var dataLicense: String? = null
) : CommonEntity()
