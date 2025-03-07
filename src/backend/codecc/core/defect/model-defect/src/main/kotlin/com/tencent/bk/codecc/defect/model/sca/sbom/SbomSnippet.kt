package com.tencent.bk.codecc.defect.model.sca.sbom

import com.tencent.codecc.common.db.CommonEntity
import org.springframework.data.mongodb.core.mapping.Field

open class SbomSnippet(
    /**
     * SBOM ID
     */
    @Field("element_id")
    var elementId: String? = null,

    /**
     * SBOM 文件
     */
    @Field("snippet_from_file")
    var snippetFromFile: String? = null,

    /**
     * SBOM 代码位置
     */
    @Field("ranges")
    var ranges: List<SbomStartEndPointer>? = null
) : CommonEntity()
