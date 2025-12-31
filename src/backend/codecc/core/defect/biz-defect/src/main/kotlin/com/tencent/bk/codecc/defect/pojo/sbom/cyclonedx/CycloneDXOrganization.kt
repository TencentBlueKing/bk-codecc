package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty

class CycloneDXOrganization(

    /**
     * BOM引用标识符
     */
    @JsonProperty("bom-ref")
    var bomRef: String? = null,

    /**
     * 组织名称
     */
    @JsonProperty("name")
    var name: String? = null,

    /**
     * 组织相关URL链接列表
     */
    @JsonProperty("url")
    var url: List<String>? = null,

    /**
     * 联系人
     */
    @JsonProperty("contact")
    var contact: List<CycloneDXAuthor>? = null,
)
