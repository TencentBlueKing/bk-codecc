package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty

class CycloneDXLicenses(
    /**
     * 证书
     */
    @JsonProperty("license")
    var license: CycloneDXLicense? = null
)
