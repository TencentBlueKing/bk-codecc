package com.tencent.bk.codecc.defect.model.sca.sbom

import org.springframework.data.mongodb.core.mapping.Field

/**
 * SBOM 生成信息
 */
open class SbomPackage(
    /**
     * SBOM 校验码
     */
    @Field("checksums")
    var checksums: List<SbomChecksum>? = null,
    /**
     * 严重程度类别：未知（0），严重（1），一般（2），提示（4）
     */
    @Field("severity")
    var severity: Int = 0,
    /**
     * 描述
     */
    @Field("description")
    var description: String? = null,
    /**
     * 下载地址
     */
    @Field("download_location")
    var downloadLocation: String? = null,
    /**
     * 主页地址
     */
    @Field("homepage")
    var homepage: String? = null,
    /**
     * 组织、发起者
     */
    @Field("originator")
    var originator: String? = null,
    /**
     * 包文件名
     */
    @Field("package_file_name")
    var packageFileName: String? = null,
    /**
     * 源信息
     */
    @Field("source_info")
    var sourceInfo: String? = null,
    /**
     * 摘要
     */
    @Field("summary")
    var summary: String? = null,
    /**
     * 供应商
     */
    @Field("supplier")
    var supplier: String? = null,
    /**
     * 版本信息
     */
    @Field("files_analyzed")
    var filesAnalyzed: Boolean? = null,
    /**
     * 版本信息
     */
    @Field("external_refs")
    var externalRefs: List<SbomExternalRef>? = null,
    /**
     * 确定的证书
     */
    @Field("license_concluded")
    var licenseConcluded: String? = null,
    /**
     * 声明的证书
     */
    @Field("license_declared")
    var licenseDeclared: String? = null,
    /**
     * 根据依赖关系分析出来的依赖深度，默认：1
     */
    @Field("depth")
    var depth: Int = 1

) : SbomPackageBase()
