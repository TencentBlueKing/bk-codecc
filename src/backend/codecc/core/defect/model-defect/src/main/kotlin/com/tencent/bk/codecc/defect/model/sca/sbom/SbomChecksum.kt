package com.tencent.bk.codecc.defect.model.sca.sbom

import org.springframework.data.mongodb.core.mapping.Field

/**
 * SBOM 校验码信息
 */
class SbomChecksum {
    /**
     * 算法
     */
    @Field("algorithm")
    val algorithm: String? = null

    /**
     * 校验码值
     */
    @Field("checksum_value")
    val checksumValue: String? = null
}
