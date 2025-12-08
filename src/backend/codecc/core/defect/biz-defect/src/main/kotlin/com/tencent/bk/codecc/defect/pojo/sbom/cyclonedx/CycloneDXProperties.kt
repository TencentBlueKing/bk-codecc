package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.constant.ComConstants.EMPTY_STRING

class CycloneDXProperties(
    /**
     * 名称
     */
    @JsonProperty("name")
    var name: String = EMPTY_STRING,

    /**
     * 值
     */
    @JsonProperty("value")
    var value: String = EMPTY_STRING
)
