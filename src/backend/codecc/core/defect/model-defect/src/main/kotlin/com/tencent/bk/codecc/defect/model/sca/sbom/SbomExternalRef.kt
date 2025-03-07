package com.tencent.bk.codecc.defect.model.sca.sbom

import org.springframework.data.mongodb.core.mapping.Field

/**
 * SBOM 外部引用
 */
class SbomExternalRef {
    /**
     * 引用分类
     */
    @Field("reference_category")
    var referenceCategory: String? = null

    /**
     * 引用类型
     */
    @Field("reference_type")
    var referenceType: String? = null

    /**
     * 引用地址
     */
    @Field("reference_locator")
    var referenceLocator: String? = null
}
