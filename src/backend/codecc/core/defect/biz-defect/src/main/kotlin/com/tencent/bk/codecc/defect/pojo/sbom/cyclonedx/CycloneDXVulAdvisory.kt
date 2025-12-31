package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.constant.ComConstants.EMPTY_STRING

class CycloneDXVulAdvisory(

    /**
     * 漏洞公告的URL链接
     */
    @JsonProperty("url")
    var url: String = EMPTY_STRING,

    /**
     * 漏洞公告的标题
     */
    @JsonProperty("title")
    var title: String? = null
)
