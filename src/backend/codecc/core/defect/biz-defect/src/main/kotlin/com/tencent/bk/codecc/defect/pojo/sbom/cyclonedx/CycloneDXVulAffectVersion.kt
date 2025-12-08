package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty

class CycloneDXVulAffectVersion(

    /**
     * 受漏洞影响的组件版本
     */
    @JsonProperty("version")
    var version: String? = null,

    /**
     * 受漏洞影响的版本范围
     */
    @JsonProperty("range")
    var range: String? = null,

    /**
     * 漏洞影响状态
     */
    @JsonProperty("status")
    var status: String? = null
)
