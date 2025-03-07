package com.tencent.bk.codecc.defect.model.sca.sbom

import org.springframework.data.mongodb.core.mapping.Field

/**
 * SBOM 创建信息
 */
class SbomCreationInfo {
    /**
     * 评论
     */
    @Field("comment")
    var comment: String? = null

    /**
     * 生成时间
     */
    @Field("created")
    var created: String? = null

    /**
     * 生成时间
     */
    @Field("created_timestamp")
    var createdTimestamp: Long? = null

    /**
     * 创建人
     */
    @Field("creators")
    var creators: List<String>? = null

    /**
     * 使用的证书版本
     */
    @Field("license_list_version")
    var licenseListVersion: String? = null
}
