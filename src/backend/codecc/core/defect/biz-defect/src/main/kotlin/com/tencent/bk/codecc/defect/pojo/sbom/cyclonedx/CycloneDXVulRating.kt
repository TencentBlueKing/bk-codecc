package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty

class CycloneDXVulRating(

    /**
     * 漏洞评分来源
     */
    @JsonProperty("source")
    var source: CycloneDXVulSource? = null,

    /**
     * 漏洞评分值
     */
    @JsonProperty("score")
    var score: Double? = null,

    /**
     * 漏洞严重程度
     */
    @JsonProperty("severity")
    var severity: String? = null,

    /**
     * 评分方法
     */
    @JsonProperty("method")
    var method: String? = null,

    /**
     * 漏洞攻击向量
     */
    @JsonProperty("vector")
    var vector: String? = null,

    /**
     * 评分依据或说明
     */
    @JsonProperty("justification")
    var justification: String? = null
)
