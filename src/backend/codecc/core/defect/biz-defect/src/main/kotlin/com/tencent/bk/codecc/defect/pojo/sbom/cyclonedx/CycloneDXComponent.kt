package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.constant.ComConstants.EMPTY_STRING

class CycloneDXComponent(
    /**
     * 类型
     */
    @JsonProperty("type")
    var type: String = EMPTY_STRING,

    /**
     * 子类型
     */
    @JsonProperty("mime-type")
    var mimeType: String? = null,

    /**
     * BOM引用标识符
     */
    @JsonProperty("bom-ref")
    var bomRef: String? = null,

    /**
     * 制造商信息
     */
    @JsonProperty("supplier")
    var supplier: CycloneDXOrganization? = null,

    /**
     * 制造商信息
     */
    @JsonProperty("manufacturer")
    var manufacturer: CycloneDXOrganization? = null,

    /**
     * 作者
     */
    @JsonProperty("authors")
    var authors: List<CycloneDXAuthor>? = null,

    /**
     * 发布者信息
     */
    @JsonProperty("publisher")
    var publisher: String? = null,

    /**
     * 组件所属组
     */
    @JsonProperty("group")
    var group: String? = null,

    /**
     * 组件名称
     */
    @JsonProperty("name")
    var name: String = EMPTY_STRING,

    /**
     * 组件版本
     */
    @JsonProperty("version")
    var version: String? = null,

    /**
     * 组件描述
     */
    @JsonProperty("description")
    var description: String? = null,

    /**
     * 组件作用范围
     */
    @JsonProperty("scope")
    var scope: String? = null,

    /**
     * 哈希值列表
     */
    @JsonProperty("hashes")
    var hashes: List<CycloneDXHash>? = null,

    /**
     * 许可证信息
     */
    @JsonProperty("licenses")
    var licenses: List<CycloneDXLicenses>? = null,

    /**
     * 版权信息
     */
    @JsonProperty("copyright")
    var copyright: String? = null,

    /**
     * CPE标识符
     */
    @JsonProperty("cpe")
    var cpe: String? = null,

    /**
     * PURL标识符
     */
    @JsonProperty("purl")
    var purl: String? = null,

    /**
     * 外部引用列表
     */
    @JsonProperty("externalReferences")
    var externalReferences: List<CycloneDXExtReference>? = null,

    /**
     * 额外信息
     */
    @JsonProperty("properties")
    var properties: List<CycloneDXProperties>? = null,

    /**
     * 标签列表
     */
    @JsonProperty("tags")
    var tags: List<String>? = null

)
