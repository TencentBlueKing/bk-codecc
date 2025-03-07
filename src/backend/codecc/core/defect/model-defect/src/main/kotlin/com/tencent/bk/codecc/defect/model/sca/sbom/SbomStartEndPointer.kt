package com.tencent.bk.codecc.defect.model.sca.sbom

import org.springframework.data.mongodb.core.mapping.Field

class SbomStartEndPointer(
    /**
     * 元素ID
     */
    @Field("start_pointer")
    val startPointer: SbomPointer? = null,
    /**
     * 关联的元素ID
     */
    @Field("end_pointer")
    val endPointer: SbomPointer? = null
)
