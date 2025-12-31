package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty

class CycloneDXMetadata(
    /**
     * Create Time
     */
    @JsonProperty("timestamp")
    var timestamp: String? = null,

    /**
     * 版本
     */
    @JsonProperty("tools")
    var tools: CycloneDXTools? = null,

    /**
     * 制造商信息
     */
    @JsonProperty("manufacturer")
    var manufacturer: CycloneDXOrganization? = null,

    /**
     * 作者
     */
    @JsonProperty("authors")
    var authors: List<CycloneDXAuthor>? = null,

    /**
     * 供应商信息
     */
    @JsonProperty("supplier")
    var supplier: CycloneDXOrganization? = null,

    /**
     * 许可证信息
     */
    @JsonProperty("licenses")
    var licenses: List<CycloneDXLicenses>? = null,

    /**
     * 额外信息
     */
    @JsonProperty("properties")
    var properties: List<CycloneDXProperties>? = null
)
