package com.tencent.bk.codecc.defect.model.sca

import com.tencent.bk.codecc.defect.model.sca.sbom.SbomSnippet
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.Sharded

/**
 * SBOM 关联关系
 */
@CompoundIndexes(
    CompoundIndex(name = "idx_task_id_1_tool_name_1_status_1_element_id_1",
        def = "{'task_id':1, 'tool_name':1, 'status':1, 'element_id':1}",background = true),
)
@Sharded(shardKey = ["task_id"])
@Document(collection = "t_sca_sbom_snippet")
class SCASbomSnippetEntity(
    /**
     * 任务ID
     */
    @Field("task_id")
    var taskId: Long,
    /**
     * 工具名称
     */
    @Field("tool_name")
    var toolName: String,
    /**
     * 状态（0：enabled,1: disabled）
     */
    @Field("status")
    var status: Int,
    /**
     * fixed的构建ID
     */
    @Field("fixed_build_id")
    var fixedBuildId: String? = null,
    /**
     * 最后更新人
     */
    @Field("last_update_author")
    var lastUpdateAuthor: String? = null,
    /**
     * 最后更新时间
     */
    @Field("last_update_time")
    var lastUpdateTime: Long = 0L,

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
) : SbomSnippet()
