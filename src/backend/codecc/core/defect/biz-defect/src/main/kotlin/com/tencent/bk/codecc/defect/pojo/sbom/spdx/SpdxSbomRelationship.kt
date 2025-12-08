package com.tencent.bk.codecc.defect.pojo.sbom.spdx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.bk.codecc.defect.model.sca.SCASbomRelationshipEntity
import com.tencent.devops.common.util.BeanUtils

/**
 * SBOM 关联关系
 */
open class SpdxSbomRelationship(
    /**
     * 元素ID
     */
    @JsonProperty("spdxElementId")
    val elementId: String? = null,
    /**
     * 关联的元素ID
     */
    @JsonProperty("relatedSpdxElement")
    val relatedElement: String? = null,
    /**
     * 关联关系
     */
    @JsonProperty("relationshipType")
    val relationshipType: String? = null
) {
    fun getSbomRelationship(taskId: Long, toolName: String): SCASbomRelationshipEntity {
        val sbomRelationship = SCASbomRelationshipEntity(taskId, toolName)
        BeanUtils.copyProperties(this, sbomRelationship)
        return sbomRelationship
    }
}
