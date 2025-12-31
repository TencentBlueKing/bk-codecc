package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.constant.ComConstants.EMPTY_STRING

class CycloneDXHash(
    /**
     * 哈希算法名称
     */
    @JsonProperty("alg")
    var alg: String = EMPTY_STRING,

    /**
     * 哈希值内容
     */
    @JsonProperty("content")
    var content: String = EMPTY_STRING
)
