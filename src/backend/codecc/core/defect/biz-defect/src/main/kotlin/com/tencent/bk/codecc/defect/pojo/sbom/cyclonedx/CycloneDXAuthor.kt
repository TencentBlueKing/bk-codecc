package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty

class CycloneDXAuthor(

    /**
     * BOM引用标识符
     */
    @JsonProperty("bom-ref")
    var bomRef: String? = null,

    /**
     * 作者姓名
     */
    @JsonProperty("name")
    var name: String? = null,

    /**
     * 作者邮箱
     */
    @JsonProperty("email")
    var email: String? = null,

    /**
     * 作者电话
     */
    @JsonProperty("phone")
    var phone: String? = null
)
