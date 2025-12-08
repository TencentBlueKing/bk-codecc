package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty

class CycloneDXVulSource(

    /**
     * 漏洞来源的URL链接
     */
    @JsonProperty("url")
    var url: String? = null,

    /**
     * 漏洞来源名称
     */
    @JsonProperty("name")
    var name: String? = null
)
