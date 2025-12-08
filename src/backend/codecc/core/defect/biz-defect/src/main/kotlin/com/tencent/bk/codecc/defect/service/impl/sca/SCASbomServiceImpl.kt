package com.tencent.bk.codecc.defect.service.impl.sca

import com.tencent.bk.codecc.defect.dao.defect.mongtemplate.sca.SCASbomDao
import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.sca.BuildSCASbomPackageEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomAggregateModel
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity
import com.tencent.bk.codecc.defect.service.sca.SCASbomService
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.DefectStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SCASbomServiceImpl @Autowired constructor(
    private val scaSbomDao: SCASbomDao
) : SCASbomService {

    override fun saveSbomPackages(taskId: Long, sbomPackages: List<SCASbomPackageEntity>) {
        scaSbomDao.saveSbomPackages(taskId, sbomPackages)
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

    override fun saveSCASbomSnapshot(
        taskId: Long,
        toolName: String,
        buildId: String,
        buildEntity: BuildEntity?,
        aggregateModel: SCASbomAggregateModel
    ) {
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
    }

    override fun getSCAPackageSnapshot(
        taskId: Long,
        toolName: String,
        buildId: String
    ): List<SCASbomPackageEntity> {
        logger.info("Start building SCA BOM aggregate model for task[$taskId], tool[$toolName], build[$buildId]")
        // 获取所有的快照
        val buildScaSbomPackages = scaSbomDao.getBuildSbomPackages(taskId, toolName, buildId)

        // 获取实体
        val scaSbomPackages = if (buildScaSbomPackages.isEmpty()) {
            emptyList()
        } else {
            val packageIds = buildScaSbomPackages.map { it.id }
            scaSbomDao.findSBomEntityByIds(taskId, packageIds, SCASbomPackageEntity::class.java).also {
                logger.info("Loaded ${it.size} package entities from database")
            }
        }
        return scaSbomPackages
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SCASbomService::class.java)
    }
}
