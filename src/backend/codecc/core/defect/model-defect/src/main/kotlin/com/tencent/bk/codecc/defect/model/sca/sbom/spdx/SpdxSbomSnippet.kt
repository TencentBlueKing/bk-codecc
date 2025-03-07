package com.tencent.bk.codecc.defect.model.sca.sbom.spdx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.bk.codecc.defect.model.sca.SCASbomSnippetEntity
import com.tencent.bk.codecc.defect.model.sca.sbom.SbomStartEndPointer
import com.tencent.devops.common.constant.ComConstants.Status
import com.tencent.devops.common.util.BeanUtils

class SpdxSbomSnippet(
    /**
     * SBOM ID
     */
    @JsonProperty("SPDXID")
    var elementId: String? = null,

    /**
     * SBOM 文件
     */
    @JsonProperty("snippetFromFile")
    var snippetFromFile: String? = null,

    /**
     * SBOM 代码位置
     */
    @JsonProperty("ranges")
    var ranges: List<SbomStartEndPointer>? = null
) {
    fun getSbomSnippet(taskId: Long, toolName: String): SCASbomSnippetEntity {
        val sbomSnippet = SCASbomSnippetEntity(taskId, toolName, Status.ENABLE.value())
        BeanUtils.copyProperties(this, sbomSnippet)
        return sbomSnippet
    }
}
