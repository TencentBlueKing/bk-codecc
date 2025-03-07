package com.tencent.bk.codecc.defect.model.sca.sbom

import com.tencent.codecc.common.db.CommonEntity
import org.springframework.data.mongodb.core.mapping.Field

/**
 * SBOM 关联关系
 */
open class SbomRelationship(
    /**
     * 元素ID
     */
    @Field("element_id")
    var elementId: String? = null,
    /**
     * 关联的元素ID
     */
    @Field("related_element_id")
    var relatedElement: String? = null,
    /**
     * 关联关系
     */
    @Field("relationship_type")
    var relationshipType: String? = null
) : CommonEntity()
