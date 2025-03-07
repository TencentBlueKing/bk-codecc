package com.tencent.bk.codecc.defect.service.sca

import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomAggregateModel
import com.tencent.bk.codecc.defect.model.sca.SCASbomFileEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomInfoEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomRelationshipEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomSnippetEntity

interface SCASbomService {

    /**
     * 插入或更新SBOM基础信息
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param scaSbomInfo SBOM基础信息实体
     */
    fun upsertSbomInfoByTaskIdAndToolName(taskId: Long, toolName: String, scaSbomInfo: SCASbomInfoEntity)

    /**
     * 保存SBOM组件数据
     * @param taskId 任务ID
     * @param sbomPackages SBOM组件实体列表
     */
    fun saveSbomPackages(taskId: Long, sbomPackages: List<SCASbomPackageEntity>)

    /**
     * 保存SBOM文件信息
     * @param taskId 任务ID
     * @param sbomFiles 文件实体列表
     */
    fun saveSbomFiles(taskId: Long, sbomFiles: List<SCASbomFileEntity>)

    /**
     * 保存SBOM代码片段信息
     * @param taskId 任务ID
     * @param sbomSnippets 代码片段实体列表
     */
    fun saveSbomSnippets(taskId: Long, sbomSnippets: List<SCASbomSnippetEntity>)

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
     * 获取数据库存储的SCASbom聚合信息
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @return SCA组件聚合模型
     */
    fun getNewSCASbomAggregateModel(
        taskId: Long,
        toolName: String
    ): SCASbomAggregateModel

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
     * @param buildId 构建ID
     * @return SCA组件聚合模型
     */
    fun getSCASbomSnapshot(
        taskId: Long,
        toolName: String,
        buildId: String,
    ): SCASbomAggregateModel

    /**
     * 根据元素ID集合查询文件信息
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param ids 元素ID集合
     * @return 匹配的文件实体列表
     */
    fun getFilesByElementIds(taskId: Long, toolName: String, ids: Collection<String>): List<SCASbomFileEntity>

    /**
     * 获取任务下所有启用的文件列表
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @return 启用的文件实体列表
     */
    fun getEnableFilesByTaskIdAndToolName(taskId: Long, toolName: String): List<SCASbomFileEntity>

    /**
     * 根据元素ID集合查询代码片段
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param ids 元素ID集合
     * @return 匹配的代码片段实体列表
     */
    fun getSnippetsByElementIds(taskId: Long, toolName: String, ids: Collection<String>): List<SCASbomSnippetEntity>

    /**
     * 获取任务下所有启用的代码片段
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @return 启用的代码片段实体列表
     */
    fun getEnableSnippetsByTaskIdAndToolName(taskId: Long, toolName: String): List<SCASbomSnippetEntity>

    /**
     * 删除旧关系并插入新的关系数据
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param sbomRelations 新的关系实体列表
     */
    fun deleteAndInsertNewRelations(taskId: Long, toolName: String, sbomRelations: List<SCASbomRelationshipEntity>)

    /**
     * 更新包关系数据
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param sbomRelations 新的关系实体列表
     * @param elementIds 需要更新的元素ID集合
     */
    fun updateRelationship(
        taskId: Long,
        toolName: String,
        sbomRelations: List<SCASbomRelationshipEntity>,
        elementIds: Set<String>
    )
}
