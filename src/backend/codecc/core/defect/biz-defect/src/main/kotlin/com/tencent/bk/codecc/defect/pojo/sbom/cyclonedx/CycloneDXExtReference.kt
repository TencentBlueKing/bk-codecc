package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.constant.ComConstants.EMPTY_STRING

class CycloneDXExtReference(
    /**
     * 外部引用的URL链接
     */
    @JsonProperty("url")
    var url: String = EMPTY_STRING,

    /**
     * 外部引用的备注信息
     */
    @JsonProperty("comment")
    var comment: String? = null,

    /**
     * 外部引用的类型
     */
    @JsonProperty("type")
    var type: String = EMPTY_STRING,

    /**
     * 外部引用的哈希值列表
     */
    @JsonProperty("hashes")
    var hashes: List<CycloneDXHash>? = null
)
