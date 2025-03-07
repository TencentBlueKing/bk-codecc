package com.tencent.bk.codecc.defect.service.impl.sca

import com.tencent.bk.codecc.defect.dao.defect.mongtemplate.sca.SCASbomDao
import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.sca.BuildSCASbomFileEntity
import com.tencent.bk.codecc.defect.model.sca.BuildSCASbomInfoEntity
import com.tencent.bk.codecc.defect.model.sca.BuildSCASbomPackageEntity
import com.tencent.bk.codecc.defect.model.sca.BuildSCASbomRelationshipEntity
import com.tencent.bk.codecc.defect.model.sca.BuildSCASbomSnippetEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomAggregateModel
import com.tencent.bk.codecc.defect.model.sca.SCASbomFileEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomInfoEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomRelationshipEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomSnippetEntity
import com.tencent.bk.codecc.defect.service.sca.SCASbomService
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.DefectStatus
import com.tencent.devops.common.util.BeanUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SCASbomServiceImpl @Autowired constructor(
    private val scaSbomDao: SCASbomDao
) : SCASbomService {

    override fun upsertSbomInfoByTaskIdAndToolName(taskId: Long, toolName: String, scaSbomInfo: SCASbomInfoEntity) {
        val oldInfo = scaSbomDao.findSbomInfoByTaskIdAndToolName(scaSbomInfo.taskId, scaSbomInfo.toolName)
        if (oldInfo != null) {
            // 更新
            with(scaSbomInfo) {
                entityId = oldInfo.entityId
                createdBy = oldInfo.createdBy
                createdDate = oldInfo.createdDate
            }
        } else {
            // 新增
            scaSbomInfo.applyAuditInfoOnUpdate()
        }
        scaSbomInfo.applyAuditInfoOnUpdate()
        scaSbomDao.saveSbomInfo(scaSbomInfo)
    }

    override fun saveSbomPackages(taskId: Long, sbomPackages: List<SCASbomPackageEntity>) {
        scaSbomDao.saveSbomPackages(taskId, sbomPackages)
    }

    override fun saveSbomFiles(taskId: Long, sbomFiles: List<SCASbomFileEntity>) {
        scaSbomDao.saveSbomFiles(taskId, sbomFiles)
    }

    override fun saveSbomSnippets(taskId: Long, sbomSnippets: List<SCASbomSnippetEntity>) {
        scaSbomDao.saveSbomSnippets(taskId, sbomSnippets)
    }

    override fun getPackagesByNameAndVersions(
        taskId: Long,
        toolName: String,
        nameToVersions: Map<String, List<String>?>
    ): List<SCASbomPackageEntity> {
        if (nameToVersions.isEmpty()) {
            return emptyList()
        }
        return scaSbomDao.findPackagesByNameAndVersions(taskId, toolName, nameToVersions)
    }

    override fun getNewPackagesByTaskIdAndToolName(taskId: Long, toolName: String): List<SCASbomPackageEntity> {
        var cursor: String? = null
        val packages = mutableListOf<SCASbomPackageEntity>()
        while (true) {
            val pagePackage = scaSbomDao.findPackagesByStatusWithCursor(
                taskId, toolName, DefectStatus.NEW.value(), cursor,
                ComConstants.COMMON_NUM_10000
            )
            if (pagePackage.isEmpty()) {
                break
            }
            cursor = pagePackage.last().entityId
            packages.addAll(pagePackage)
            if (pagePackage.size < ComConstants.COMMON_NUM_10000) {
                break
            }
        }
        return packages
    }

    override fun getNewSCASbomAggregateModel(taskId: Long, toolName: String): SCASbomAggregateModel {
        val sbomInfo =
            scaSbomDao.findSbomInfoByTaskIdAndToolName(taskId, toolName) ?: SCASbomInfoEntity(taskId, toolName)
        val sbomPackages = getNewPackagesByTaskIdAndToolName(taskId, toolName)
        val sbomFiles = getEnableFilesByTaskIdAndToolName(taskId, toolName)
        val sbomSnippets = getEnableSnippetsByTaskIdAndToolName(taskId, toolName)
        val sbomRelationships = scaSbomDao.findRelationsByTaskIdAndToolName(taskId, toolName)
        return SCASbomAggregateModel(
            taskId,
            toolName,
            sbomInfo,
            sbomPackages,
            sbomFiles,
            sbomSnippets,
            sbomRelationships
        )
    }

    override fun saveSCASbomSnapshot(
        taskId: Long,
        toolName: String,
        buildId: String,
        buildEntity: BuildEntity?,
        aggregateModel: SCASbomAggregateModel
    ) {
        // SBOM信息快照
        val buildScaSbomInfo = BuildSCASbomInfoEntity(taskId, toolName, buildId, buildEntity?.buildNo)
        BeanUtils.copyProperties(aggregateModel.info, buildScaSbomInfo, "entityId")
        scaSbomDao.saveBuildSbomInfo(taskId, toolName, buildId, buildScaSbomInfo)

        // SBOMPackage快照
        if (aggregateModel.packages.isNotEmpty()) {
            val buildScaSbomPackages = mutableListOf<BuildSCASbomPackageEntity>()
            aggregateModel.packages.forEach {
                with(it) {
                    val buildScaSbomPackage = BuildSCASbomPackageEntity(
                        taskId,
                        toolName,
                        buildId,
                        buildEntity?.buildNo,
                        entityId,
                        elementId,
                        fileInfos,
                        lineNum,
                        revision,
                        branch,
                        subModule
                    )
                    buildScaSbomPackages.add(buildScaSbomPackage)
                }
            }
            scaSbomDao.saveBuildSbomPackages(taskId, toolName, buildId, buildScaSbomPackages)
        }

        // SBOMFile快照
        if (aggregateModel.files.isNotEmpty()) {
            val buildScaSbomFiles = mutableListOf<BuildSCASbomFileEntity>()
            aggregateModel.files.forEach {
                with(it) {
                    val buildScaSbomFile = BuildSCASbomFileEntity(
                        taskId,
                        toolName,
                        buildId,
                        buildEntity?.buildNo,
                        entityId,
                        elementId,
                        revision,
                        branch,
                        subModule
                    )
                    buildScaSbomFiles.add(buildScaSbomFile)
                }
            }
            scaSbomDao.saveBuildSbomFiles(taskId, toolName, buildId, buildScaSbomFiles)
        }

        // SBOMSnippet快照
        if (aggregateModel.snippets.isNotEmpty()) {
            val buildScaSbomSnippets = mutableListOf<BuildSCASbomSnippetEntity>()
            aggregateModel.snippets.forEach {
                with(it) {
                    val buildScaSbomSnippet = BuildSCASbomSnippetEntity(
                        taskId, toolName, buildId, buildEntity?.buildNo, entityId, elementId
                    )
                    buildScaSbomSnippets.add(buildScaSbomSnippet)
                }
            }
            scaSbomDao.saveBuildSbomSnippets(taskId, toolName, buildId, buildScaSbomSnippets)
        }

        // SBOMRelationship快照
        if (aggregateModel.relationships.isNotEmpty()) {
            val buildScaSbomRelationships = mutableListOf<BuildSCASbomRelationshipEntity>()
            aggregateModel.relationships.forEach {
                with(it) {
                    val buildScaSbomRelationship = BuildSCASbomRelationshipEntity(
                        taskId, toolName, buildId, buildEntity?.buildNo
                    )
                    buildScaSbomRelationship.elementId = elementId
                    buildScaSbomRelationship.relatedElement = relatedElement
                    buildScaSbomRelationship.relationshipType = relationshipType
                    buildScaSbomRelationships.add(buildScaSbomRelationship)
                }
            }
            scaSbomDao.saveBuildSbomRelationships(taskId, toolName, buildId, buildScaSbomRelationships)
        }
    }

    override fun getSCASbomSnapshot(taskId: Long, toolName: String, buildId: String): SCASbomAggregateModel {
        logger.info("Start building SCA BOM aggregate model for task[$taskId], tool[$toolName], build[$buildId]")
        // 获取所有的快照
        val buildScaSbomInfo = scaSbomDao.getBuildSbomInfo(taskId, toolName, buildId)
            ?: BuildSCASbomInfoEntity(taskId, toolName, buildId)
        val buildScaSbomPackages = scaSbomDao.getBuildSbomPackages(taskId, toolName, buildId)
        val buildScaSbomFiles = scaSbomDao.getBuildSbomFiles(taskId, toolName, buildId)
        val buildScaSbomSnippets = scaSbomDao.getBuildSbomSnippets(taskId, toolName, buildId)
        val buildScaSbomRelationships = scaSbomDao.getBuildSbomRelationships(taskId, toolName, buildId)

        // 获取实体
        val scaSbomInfo = SCASbomInfoEntity(taskId, toolName)
        BeanUtils.copyProperties(buildScaSbomInfo, scaSbomInfo, "entityId")
        val scaSbomPackages = if (buildScaSbomPackages.isEmpty()) {
            emptyList()
        } else {
            val packageIds = buildScaSbomPackages.map { it.id }
            scaSbomDao.findSBomEntityByIds(taskId, packageIds, SCASbomPackageEntity::class.java).also {
                logger.info("Loaded ${it.size} package entities from database")
            }
        }
        val scaSbomFiles = if (buildScaSbomFiles.isEmpty()) {
            emptyList()
        } else {
            val fileIds = buildScaSbomFiles.map { it.id }
            scaSbomDao.findSBomEntityByIds(taskId, fileIds, SCASbomFileEntity::class.java).also {
                logger.info("Loaded ${it.size} file entities from database")
            }
        }
        val scaSbomSnippets = if (buildScaSbomSnippets.isEmpty()) {
            emptyList()
        } else {
            val snippetIds = buildScaSbomSnippets.map { it.id }
            scaSbomDao.findSBomEntityByIds(taskId, snippetIds, SCASbomSnippetEntity::class.java).also {
                logger.info("Loaded ${it.size} snippet entities from database")
            }
        }
        val scaSbomRelationships = if (buildScaSbomRelationships.isEmpty()) {
            emptyList()
        } else {
            buildScaSbomRelationships.map {
                val sbomRelationship = SCASbomRelationshipEntity(it.taskId, it.toolName)
                sbomRelationship.elementId = it.elementId
                sbomRelationship.relatedElement = it.relatedElement
                sbomRelationship.relationshipType = it.relationshipType
                sbomRelationship
            }.toList().also {
                logger.info("Mapped ${it.size} relationship entities")
            }
        }
        return SCASbomAggregateModel(
            taskId,
            toolName,
            scaSbomInfo,
            scaSbomPackages,
            scaSbomFiles,
            scaSbomSnippets,
            scaSbomRelationships
        ).also {
            logger.info(
                "Successfully built SCA BOM aggregate model with " +
                    "${it.packages.size} packages, ${it.files.size} files, " +
                    "${it.snippets.size} snippets, ${it.relationships.size} relationships"
            )
        }
    }

    override fun getFilesByElementIds(
        taskId: Long,
        toolName: String,
        ids: Collection<String>
    ): List<SCASbomFileEntity> = scaSbomDao.findFilesByTaskAndToolNameAndElementIds(taskId, toolName, ids)

    override fun getEnableFilesByTaskIdAndToolName(taskId: Long, toolName: String): List<SCASbomFileEntity> {
        var cursor: String? = null
        val files = mutableListOf<SCASbomFileEntity>()
        while (true) {
            val pageFiles = scaSbomDao.findFilesByTaskAndToolNameAndStatusWithCursor(
                taskId, toolName, ComConstants.Status.ENABLE.value(), cursor,
                ComConstants.COMMON_NUM_10000
            )
            if (pageFiles.isEmpty()) {
                break
            }
            cursor = pageFiles.last().elementId
            files.addAll(pageFiles)
            if (pageFiles.size < ComConstants.COMMON_NUM_10000) {
                break
            }
        }
        return files
    }

    override fun getSnippetsByElementIds(
        taskId: Long,
        toolName: String,
        ids: Collection<String>
    ): List<SCASbomSnippetEntity> = scaSbomDao.findSnippetsByTaskAndToolNameAndElementIds(taskId, toolName, ids)

    override fun getEnableSnippetsByTaskIdAndToolName(taskId: Long, toolName: String): List<SCASbomSnippetEntity> {
        var cursor: String? = null
        val snippets = mutableListOf<SCASbomSnippetEntity>()
        while (true) {
            val pageSnippets = scaSbomDao.findSnippetByTaskAndToolNameAndStatusWithCursor(
                taskId, toolName, ComConstants.Status.ENABLE.value(), cursor,
                ComConstants.COMMON_NUM_10000
            )
            if (pageSnippets.isEmpty()) {
                break
            }
            cursor = pageSnippets.last().elementId
            snippets.addAll(pageSnippets)
            if (pageSnippets.size < ComConstants.COMMON_NUM_10000) {
                break
            }
        }
        return snippets
    }

    override fun deleteAndInsertNewRelations(
        taskId: Long,
        toolName: String,
        sbomRelations: List<SCASbomRelationshipEntity>
    ) {
        scaSbomDao.removeRelationsByTaskIdAndToolName(taskId, toolName)
        if (sbomRelations.isEmpty()) {
            return
        }
        scaSbomDao.saveSbomRelations(taskId, sbomRelations)
    }

    override fun updateRelationship(
        taskId: Long,
        toolName: String,
        sbomRelations: List<SCASbomRelationshipEntity>,
        elementIds: Set<String>
    ) {
        // 删除所有已经关闭的Element关系
        scaSbomDao.removeRelationsByTaskIdAndToolNameAndElementIds(taskId, toolName, elementIds)
        // 更新sbomRelations
        if (sbomRelations.isEmpty()) {
            return
        }
        scaSbomDao.saveSbomRelations(taskId, sbomRelations)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SCASbomService::class.java)
    }
}
