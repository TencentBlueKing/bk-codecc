package com.tencent.bk.codecc.defect.model.sca.sbom

import org.springframework.data.mongodb.core.mapping.Field

class SbomPointer(
    /**
     * 元素ID
     */
    @Field("line_number")
    val lineNumber: Int? = null,
    /**
     * 关联的元素ID
     */
    @Field("reference")
    val reference: String? = null
)
