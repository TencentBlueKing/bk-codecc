package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.constant.ComConstants.EMPTY_STRING

class CycloneDXDependency(
    /**
     * 依赖项的引用标识
     */
    @JsonProperty("ref")
    var ref: String = EMPTY_STRING,

    /**
     * 当前组件依赖的其他组件引用列表
     */
    @JsonProperty("dependsOn")
    var dependsOn: List<String>? = null,

    /**
     * 当前组件提供的功能或资源列表
     */
    @JsonProperty("provides")
    var provides: List<String>? = null
)
