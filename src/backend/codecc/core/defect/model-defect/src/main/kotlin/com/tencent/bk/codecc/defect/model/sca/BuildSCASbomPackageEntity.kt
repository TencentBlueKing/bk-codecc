package com.tencent.bk.codecc.defect.model.sca

import com.tencent.codecc.common.db.CommonEntity
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.Sharded

/**
 * SBOM 组件生成记录
 */
@CompoundIndexes(
    CompoundIndex(
        name = "task_id_1_tool_name_1_build_id_1_id_1",
        def = "{'task_id':1, 'tool_name':1, 'build_id':1, 'id':1}",
        background = true
    )
)
@Sharded(shardKey = ["task_id"])
@Document(collection = "t_build_sbom_package")
class BuildSCASbomPackageEntity(
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
     * 构建ID
     */
    @Field("build_id")
    var buildId: String,
    /**
     * 构建编号
     */
    @Field("build_num")
    var buildNum: String? = null,
    /**
     * SBOM 包ID
     */
    @Field("id")
    var id: String,
    /**
     * SBOM 元素ID
     */
    @Field("element_id")
    var elementId: String? = null,
    /**
     * 引入的文件信息列表
     */
    @Field("file_infos")
    var fileInfos: List<SCAPackageFileInfo>? = null,
    /**
     * 行号
     */
    @Field("line_num")
    var lineNum: Int? = null,
    /**
     * CommitID
     */
    @Field("revision")
    var revision: String? = null,
    /**
     * 分支名
     */
    @Field("branch")
    var branch: String? = null,
    /**
     * 子模块
     */
    @Field("sub_module")
    var subModule: String? = null
) : CommonEntity()
