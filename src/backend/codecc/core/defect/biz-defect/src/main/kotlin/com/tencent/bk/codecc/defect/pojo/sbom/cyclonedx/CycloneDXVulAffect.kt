package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.constant.ComConstants.EMPTY_STRING

class CycloneDXVulAffect(

    /**
     * 漏洞影响的组件引用标识
     */
    @JsonProperty("ref")
    var ref: String = EMPTY_STRING,

    /**
     * 漏洞影响的版本范围列表
     */
    @JsonProperty("versions")
    var versions: List<CycloneDXVulAffectVersion>? = null
)
