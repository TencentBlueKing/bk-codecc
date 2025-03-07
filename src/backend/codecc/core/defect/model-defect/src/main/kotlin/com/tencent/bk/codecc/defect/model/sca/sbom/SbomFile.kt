package com.tencent.bk.codecc.defect.model.sca.sbom

import com.tencent.codecc.common.db.CommonEntity
import org.springframework.data.mongodb.core.mapping.Field

open class SbomFile(
    /**
     * SBOM ID
     */
    @Field("element_id")
    var elementId: String? = null,

    /**
     * SBOM 文件
     */
    @Field("file_name")
    var fileName: String? = null,

    /**
     * SBOM 文件
     */
    @Field("file_rel_path")
    var fileRelPath: String? = null,

    /**
     * SBOM 文件
     */
    @Field("file_path")
    var filePath: String? = null,

    /**
     * SBOM 校验码
     */
    @Field("checksums")
    var checksums: List<SbomChecksum>? = null,
    /**
     * 文件类型
     */
    @Field("file_types")
    var fileTypes: List<String>? = null,

    /**
     * 压缩包文件路径
     */
    @Field("data_file_name")
    var dataFileName: String? = null,

) : CommonEntity()
