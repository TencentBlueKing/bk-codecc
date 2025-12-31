package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.constant.ComConstants.EMPTY_STRING

class CycloneDXVulReference(

    /**
     * 漏洞引用标识符
     */
    @JsonProperty("id")
    var id: String = EMPTY_STRING,

    /**
     * 漏洞来源信息
     */
    @JsonProperty("source")
    var source: CycloneDXVulSource = CycloneDXVulSource()
)
