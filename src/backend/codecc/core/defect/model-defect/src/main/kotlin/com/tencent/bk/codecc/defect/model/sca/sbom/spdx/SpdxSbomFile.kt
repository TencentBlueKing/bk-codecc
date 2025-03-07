package com.tencent.bk.codecc.defect.model.sca.sbom.spdx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.bk.codecc.defect.model.sca.SCASbomFileEntity
import com.tencent.bk.codecc.defect.model.sca.sbom.SbomChecksum
import com.tencent.devops.common.constant.ComConstants.Status
import com.tencent.devops.common.util.BeanUtils

class SpdxSbomFile(
    /**
     * SBOM ID
     */
    @JsonProperty("SPDXID")
    var elementId: String? = null,
    /**
     * SBOM 文件
     */
    @JsonProperty("fileRelPath")
    var fileRelPath: String? = null,
    /**
     * SBOM 文件
     */
    @JsonProperty("filePath")
    var filePath: String? = null,
    /**
     * SBOM 校验码
     */
    @JsonProperty("checksums")
    var checksums: List<SbomChecksum>? = null,
    /**
     * 文件类型
     */
    @JsonProperty("fileTypes")
    var fileTypes: List<String>? = null,

    /**
     * 文件类型
     */
    @JsonProperty("__dataFileName")
    var dataFileName: String? = null
) {
    fun getSbomFile(taskId: Long, toolName: String): SCASbomFileEntity {
        val sbomFile = SCASbomFileEntity(taskId, toolName, Status.ENABLE.value())
        BeanUtils.copyProperties(this, sbomFile)
        return sbomFile
    }
}
