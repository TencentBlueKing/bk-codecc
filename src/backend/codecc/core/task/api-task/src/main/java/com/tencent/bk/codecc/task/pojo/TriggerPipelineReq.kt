package com.tencent.bk.codecc.task.pojo

import com.tencent.devops.common.constant.ComConstants
import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "流水线触发请求体")
data class TriggerPipelineReq(
    @get:Schema(description = "仓库路径")
    val gitUrl: String?,
    @get:Schema(description = "分支")
    val branch: String?,
    @get:Schema(description = "commitId")
    val commitId: String?,
    @get:Schema(description = "版本号")
    val revision: String?,
    @get:Schema(description = "工蜂项目id")
    val gongfengProjectId: Int?,
    @get:Schema(description = "逻辑仓标识")
    val logicRepo: String? = null,
    @get:Schema(description = "仓库类型")
    val repoType: String?,
    @get:Schema(description = "是否显示告警")
    val defectDisplay: Boolean?,
    @get:Schema(description = "是否校验工蜂项目")
    val checkGongfengProject: Boolean? = false,
    @get:Schema(description = "是否采用yml文件进行流水线编排")
    val useYml: Boolean? = false,
    @get:Schema(description = "指定规则集包类型")
    val checkerSetPackageType: String?,
    @get:Schema(description = "指定开源扫描规则集模型")
    val checkerSetRange: List<OpenSourceCheckerSetModel>?,
    @get:Schema(description = "指定扫描集群")
    val codeccDispatchRoute: ComConstants.CodeCCDispatchRoute?,
    @get:Schema(description = "codecc流水线模型")
    val codeCCPipelineReq: CodeCCPipelineReq,
    @get:Schema(description = "支持指定git仓库目录，多个目录用英文逗号分隔")
    val includePath: String?,
    @get:Schema(description = "扫描任务owner")
    val taskOwner: String? = null,
    @get:Schema(description = "仓库 namespace owner")
    val namespaceOwners: String? = null,
    @get:Schema(description = "仓库 project owner")
    val projectOwners: String? = null,
    @get:Schema(description = "一级部门ID")
    val bgId: Long? = null,
    @get:Schema(description = "二级部门ID")
    val deptId: Long? = null,
    @get:Schema(description = "三级部门ID")
    val centerId: Long? = null,
    @get:Schema(description = "四级部门ID")
    val groupId: Long? = null,
    @get:Schema(description = "是否忽略分支参数来识别同一个任务")
    val branchIgnore: Boolean? = null,
    @get:Schema(description = "额外的仓库信息")
    val extRepoInfo: Map<String, Any>? = null
) {
    constructor(
        gitUrl: String?,
        branch: String?,
        gongfengProjectId: Int?,
        logicRepo: String?,
        repoType: String?,
        defectDisplay: Boolean?,
        checkGongfengProject: Boolean?,
        useYml: Boolean?,
        checkerSetRange: List<OpenSourceCheckerSetModel>?,
        codeccDispatchRoute: ComConstants.CodeCCDispatchRoute?,
        codeCCPipelineReq: CodeCCPipelineReq
    ) : this(
        gitUrl,
        branch,
        null,
        null,
        gongfengProjectId,
        logicRepo,
        repoType,
        defectDisplay,
        checkGongfengProject,
        useYml,
        null,
        checkerSetRange,
        codeccDispatchRoute,
        codeCCPipelineReq,
        null
    )

    constructor(
        gitUrl: String?,
        branch: String?,
        gongfengProjectId: Int?,
        taskOwner: String?,
        namespaceOwners: String?,
        projectOwners: String?,
        codeccDispatchRoute: ComConstants.CodeCCDispatchRoute?,
        extRepoInfo: Map<String, Any>?,
        codeCCPipelineReq: CodeCCPipelineReq
    ) : this(
        gitUrl,
        branch,
        null,
        null,
        gongfengProjectId,
        null,
        null,
        null,
        false,
        false,
        null,
        null,
        codeccDispatchRoute,
        codeCCPipelineReq,
        null,
        taskOwner,
        namespaceOwners,
        projectOwners,
        null,
        null,
        null,
        null,
        null,
        extRepoInfo
    )
}
