package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty

class CycloneDXTools(
    /**
     * 组件
     */
    @JsonProperty("components")
    var components: List<CycloneDXComponent>? = null
)
