package com.tencent.bk.codecc.defect.model.sca.sbom

import com.tencent.codecc.common.db.CommonEntity
import org.springframework.data.mongodb.core.mapping.Field

open class SbomPackageBase(
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

) : CommonEntity() {
    fun getPackageId(): String? {
        return if (version.isNullOrEmpty()) {
            name
        } else {
            "$name:$version"
        }
    }
}
