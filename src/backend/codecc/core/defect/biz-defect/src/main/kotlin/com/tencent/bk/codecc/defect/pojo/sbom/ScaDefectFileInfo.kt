package com.tencent.bk.codecc.defect.pojo.sbom

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity
import com.tencent.bk.codecc.defect.model.sca.vulnerability.SCAVulnerabilityInfo
import com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx.CycloneDX
import com.tencent.bk.codecc.defect.pojo.sbom.spdx.SpdxSbomInfo
import com.tencent.devops.common.constant.DefectConstants

/**
 * SCA缺陷文件信息
 */
class ScaDefectFileInfo(
    @JsonProperty("sbom")
    var sbom: Any? = null,
    @JsonProperty("spdxSbom")
    var spdxSbom: SpdxSbomInfo? = null,
    @JsonProperty("cycloneDXSbom")
    var cycloneDXSbom: CycloneDX? = null,
    @JsonProperty("packages")
    var packages: List<SCASbomPackageEntity>? = null,
    @JsonProperty("licenses")
    var licenses: List<SCALicenseEntity>? = null,
    @JsonProperty("vulnerabilities")
    var vulnerabilities: List<SCAVulnerabilityInfo>? = null,
    @JsonProperty("analysisPackageFromSbom")
    var analysisPackageFromSbom: Boolean? = true,
    @JsonProperty("analysisLicenseFromSbom")
    var analysisLicenseFromSbom: Boolean? = true,
    @JsonProperty("analysisVulnerabilityFromSbom")
    var analysisVulnerabilityFromSbom: Boolean? = false,
    @JsonProperty("sbomType")
    var sbomType: String? = "spdx",
    /**
     * 增量文件列表
     */
    @JsonProperty("incrementalFiles")
    var incrementalFiles: List<String>? = null
) {

    fun sbomType(): DefectConstants.SCASbomType {
        return if (sbomType == null || sbomType.equals(DefectConstants.SCASbomType.SPDX.name, true)) {
            DefectConstants.SCASbomType.SPDX
        } else if (sbomType.equals(DefectConstants.SCASbomType.CYCLONEDX.name, true)) {
            DefectConstants.SCASbomType.CYCLONEDX
        } else {
            DefectConstants.SCASbomType.SPDX
        }
    }
}
