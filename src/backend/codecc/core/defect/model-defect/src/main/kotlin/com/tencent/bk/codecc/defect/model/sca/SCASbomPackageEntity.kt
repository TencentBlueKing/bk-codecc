package com.tencent.bk.codecc.defect.model.sca

import com.tencent.bk.codecc.defect.model.sca.sbom.SbomPackage
import com.tencent.devops.common.constant.ComConstants
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.Sharded


/**
 * SBOM 生成信息
 */
@CompoundIndexes(
    CompoundIndex(name = "idx_task_id_1_tool_name_1_status_1", def = "{'task_id':1, 'tool_name':1, 'status':1}",
        background = true),
    CompoundIndex(name = "idx_task_id_1_tool_name_1_name_1_version_1",
        def = "{'task_id':1, 'tool_name':1, 'name':1, 'version':1}",
        background = true)
)
@Sharded(shardKey = ["task_id"])
@Document(collection = "t_sca_sbom_package")
class SCASbomPackageEntity(
    /**
     * 任务ID
     */
    @Field("task_id")
    var taskId: Long = 0L,
    /**
     * 工具名称
     */
    @Field("tool_name")
    var toolName: String = ComConstants.EMPTY_STRING,
    /**
     * 状态
     */
    @Field("status")
    var status: Int = ComConstants.DefectStatus.NEW.value(),

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
    var subModule: String? = null,
    /**
     * 链接
     */
    @Field("url")
    var url: String? = null,
    /**
     * repoId
     */
    @Field("repo_id")
    var repoId: String? = null,
    /**
     * 作者列表
     */
    @Field("author")
    var author: List<String>? = null,
    /**
     * 最后更新时间
     */
    @Field("last_update_time")
    var lastUpdateTime: Long = 0L,

    /**
     * 告警忽略时间
     */
    @Field("ignore_time")
    var ignoreTime: Long? = null,

    /**
     * 告警忽略原因类型
     */
    @Field("ignore_reason_type")
    var ignoreReasonType: Int? = null,

    /**
     * 告警忽略原因
     */
    @Field("ignore_reason")
    var ignoreReason: String? = null,
    /**
     * 告警忽略操作人
     */
    @Field("ignore_author")
    var ignoreAuthor: String? = null,

    /**
     * 告警是否被标记为已修改的标志，checkbox for developer, 0 is normal, 1 is tag, 2 is prompt
     */
    @Field("mark")
    var mark: Int = 0,

    /**
     * 告警被标记为已修改的时间
     */
    @Field("mark_time")
    var markTime: Long = 0L,

    /**
     * 创建时的构建号
     */
    @Field("create_build_number")
    var createBuildNumber: String? = null,

    /**
     * 告警创建时间
     */
    @Field("create_time")
    var createTime: Long? = null,

    /**
     * 修复时的构建号
     */
    @Field("fixed_build_number")
    var fixedBuildNumber: String? = null,

    /**
     * 修复时的构建ID
     */
    @Field("fixed_build_id")
    var fixedBuildId: String? = null,

    /**
     * 告警修复时间
     */
    @Field("fixed_time")
    var fixedTime: Long? = null,

    /**
     * 组件使用到的证书
     * licenseConcluded 与 licenseDeclared 的集合
     */
    @Field("licenses")
    var licenses: List<String> = emptyList(),
) : SbomPackage() {
    constructor(taskId: Long, toolName: String, status: Int) : this(
        taskId, toolName, status, emptyList()
    )
}
