package com.tencent.bk.codecc.defect.service.sca

import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomAggregateModel
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity

interface SCASbomService {

    /**
     * 保存SBOM组件数据
     * @param taskId 任务ID
     * @param sbomPackages SBOM组件实体列表
     */
    fun saveSbomPackages(taskId: Long, sbomPackages: List<SCASbomPackageEntity>)

    /**
     * 根据组件名称和版本查询包信息
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param nameToVersions 组件名称与版本映射表
     * @return 匹配的包实体列表
     */
    fun getPackagesByNameAndVersions(
        taskId: Long,
        toolName: String,
        nameToVersions: Map<String, List<String>?>
    ): List<SCASbomPackageEntity>

    /**
     * 获取指定任务的新增包列表
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @return 新增包实体列表
     */
    fun getNewPackagesByTaskIdAndToolName(
        taskId: Long,
        toolName: String
    ): List<SCASbomPackageEntity>

    /**
     * 保存SCA组件快照
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param buildId 构建ID
     * @param buildEntity 构建实体
     * @param aggregateModel 组件聚合模型
     */
    fun saveSCASbomSnapshot(
        taskId: Long,
        toolName: String,
        buildId: String,
        buildEntity: BuildEntity?,
        aggregateModel: SCASbomAggregateModel
    )

    /**
     * 获取指定构建版本的SCA组件快照
     * @param taskId 任务ID
     * @param toolName 工具名称
//     * @param buildId 构建ID
     * @return SCA组件列表
     */
    fun getSCAPackageSnapshot(
        taskId: Long,
        toolName: String,
        buildId: String,
    ): List<SCASbomPackageEntity>
}
