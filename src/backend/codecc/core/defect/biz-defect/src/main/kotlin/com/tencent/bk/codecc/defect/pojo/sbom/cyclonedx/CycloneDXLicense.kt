package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty

class CycloneDXLicense(
    /**
     * BOM引用标识符
     */
    @JsonProperty("bom-ref")
    var bomRef: String? = null,

    /**
     * 许可证标识符
     */
    @JsonProperty("id")
    var id: String? = null,

    /**
     * 许可证确认信息
     */
    @JsonProperty("acknowledgement")
    var acknowledgement: String? = null,

    /**
     * 许可证名称
     */
    @JsonProperty("name")
    var name: String? = null,

    /**
     * 许可证URL链接
     */
    @JsonProperty("url")
    var url: String? = null,

    /**
     * 额外信息
     */
    @JsonProperty("properties")
    var properties: List<CycloneDXProperties>? = null
)
