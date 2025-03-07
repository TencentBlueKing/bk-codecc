package com.tencent.bk.codecc.defect.model.sca.sbom.spdx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity
import com.tencent.bk.codecc.defect.model.sca.sbom.SbomChecksum
import com.tencent.bk.codecc.defect.model.sca.sbom.SbomExternalRef
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.util.BeanUtils
import org.apache.commons.lang3.StringUtils

/**
 * SBOM 生成信息
 */
class SpdxSbomPackage(
    /**
     * SBOM ID
     */
    @JsonProperty("SPDXID")
    var elementId: String? = null,
    /**
     * 包名称
     */
    @JsonProperty("name")
    var name: String? = null,
    /**
     * SBOM 校验码
     */
    @JsonProperty("checksums")
    var checksums: List<SbomChecksum>? = null,
    /**
     * 风险级别（未知0/低1/中2/高3）, 与 CodeCC定义相反，需要转换
     */
    @JsonProperty("__licenseRisk")
    var severity: Int = 0,
    /**
     * 描述
     */
    @JsonProperty("description")
    var description: String? = null,
    /**
     * 下载地址
     */
    @JsonProperty("downloadLocation")
    var downloadLocation: String? = null,
    /**
     * 主页地址
     */
    @JsonProperty("homepage")
    var homepage: String? = null,
    /**
     * 组织、发起者
     */
    @JsonProperty("originator")
    var originator: String? = null,
    /**
     * 包文件名
     */
    @JsonProperty("packageFileName")
    var packageFileName: String? = null,
    /**
     * 源信息
     */
    @JsonProperty("sourceInfo")
    var sourceInfo: String? = null,
    /**
     * 摘要
     */
    @JsonProperty("summary")
    var summary: String? = null,
    /**
     * 供应商
     */
    @JsonProperty("supplier")
    var supplier: String? = null,
    /**
     * 版本信息
     */
    @JsonProperty("version")
    var version: String? = null,
    /**
     * 版本信息
     */
    @JsonProperty("filesAnalyzed")
    var filesAnalyzed: Boolean? = null,

    /**
     * 确定的证书
     */
    @JsonProperty("licenseConcluded")
    var licenseConcluded: String? = null,
    /**
     * 声明的证书
     */
    @JsonProperty("licenseDeclared")
    var licenseDeclared: String? = null,

    /**
     * 版本信息
     */
    @JsonProperty("externalRefs")
    var externalRefs: List<SbomExternalRef>? = null

) {
    fun getSbomPackage(taskId: Long, toolName: String, status: Int): SCASbomPackageEntity {
        val sbomPackage = SCASbomPackageEntity(taskId, toolName, status)
        BeanUtils.copyProperties(this, sbomPackage)
        val licenseDeclareds = licenseDeclared?.split(PACKAGE_LICENSE_SEPARATOR)?.filter(StringUtils::isNotBlank)
            ?.map { it.trim() } ?: emptyList()
        val licenseConcludeds = licenseConcluded?.split(PACKAGE_LICENSE_SEPARATOR)?.filter(StringUtils::isNotBlank)
            ?.map { it.trim() } ?: emptyList()
        sbomPackage.licenses = licenseDeclareds + licenseConcludeds
        sbomPackage.severity = when (sbomPackage.severity) {
            SPDX_RISK_HIGH -> ComConstants.SERIOUS
            SPDX_RISK_MEDIUM -> ComConstants.NORMAL
            SPDX_RISK_LOW -> ComConstants.PROMPT
            SPDX_RISK_UNKNOWN -> ComConstants.UNKNOWN
            else -> ComConstants.UNKNOWN
        }
        return sbomPackage
    }

    companion object {
        const val PACKAGE_LICENSE_SEPARATOR: String = "OR"
        const val SPDX_RISK_HIGH: Int = 3
        const val SPDX_RISK_MEDIUM: Int = 2
        const val SPDX_RISK_LOW: Int = 1
        const val SPDX_RISK_UNKNOWN: Int = 0
    }
}
