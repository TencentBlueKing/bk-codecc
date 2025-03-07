package com.tencent.bk.codecc.defect.model.sca.sbom

import com.tencent.codecc.common.db.CommonEntity
import org.springframework.data.mongodb.core.mapping.Field

open class SbomPackageBase(
    /**
     * SBOM Package 数据库ID
     * 用于License与Vul 做关联
     */
    @Field("package_id")
    var packageId: String? = null,
    /**
     * SBOM ID
     */
    @Field("element_id")
    var elementId: String? = null,
    /**
     * 包名称
     */
    @Field("name")
    var name: String? = null,
    /**
     * 版本信息
     */
    @Field("version")
    var version: String? = null

) : CommonEntity()
