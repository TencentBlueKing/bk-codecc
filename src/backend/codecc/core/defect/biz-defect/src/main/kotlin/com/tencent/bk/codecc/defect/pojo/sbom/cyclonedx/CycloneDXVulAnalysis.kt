package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty

class CycloneDXVulAnalysis(

    /**
     * 漏洞分析状态
     */
    @JsonProperty("state")
    var state: String? = null,

    /**
     * 漏洞响应措施
     */
    @JsonProperty("response")
    var response: String? = null,

    /**
     * 漏洞详细描述
     */
    @JsonProperty("detail")
    var detail: String? = null,

    /**
     * 漏洞首次发布时间
     */
    @JsonProperty("firstIssued")
    var firstIssued: String? = null,

    /**
     * 漏洞最后更新时间
     */
    @JsonProperty("lastUpdated")
    var lastUpdated: String? = null,

    /**
     * 漏洞分析依据或说明
     */
    @JsonProperty("justification")
    var justification: String? = null
)
