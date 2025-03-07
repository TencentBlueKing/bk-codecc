package com.tencent.bk.codecc.defect.model.sca

import com.tencent.bk.codecc.defect.model.sca.sbom.SbomRelationship
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.Sharded

/**
 * SBOM 关联关系
 */
@CompoundIndexes(
    CompoundIndex(name = "idx_task_id_1_tool_name_1_element_id_1",
        def = "{'task_id':1, 'tool_name':1, 'element_id':1}", background = true),
    CompoundIndex(name = "idx_task_id_1_tool_name_1_related_element_id_1",
        def = "{'task_id':1, 'tool_name':1, 'related_element_id':1}", background = true)
)
@Sharded(shardKey = ["task_id"])
@Document(collection = "t_sca_sbom_relationship")
class SCASbomRelationshipEntity(
    /**
     * 任务ID
     */
    @Field("task_id")
    var taskId: Long,
    /**
     * 工具名称
     */
    @Field("tool_name")
    var toolName: String
) : SbomRelationship()
