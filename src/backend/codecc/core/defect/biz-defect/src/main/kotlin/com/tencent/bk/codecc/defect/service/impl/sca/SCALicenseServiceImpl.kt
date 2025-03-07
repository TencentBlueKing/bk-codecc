package com.tencent.bk.codecc.defect.service.impl.sca

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.BuildSCALicenseRepository
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.LicenseDetailRepository
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.SCALicenseRepository
import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.sca.BuildSCALicenseEntity
import com.tencent.bk.codecc.defect.model.sca.LicenseDetailEntity
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity
import com.tencent.bk.codecc.defect.service.sca.SCALicenseService
import com.tencent.devops.common.constant.ComConstants.DefectStatus
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils

@Service
class SCALicenseServiceImpl @Autowired constructor(
    private val scaLicenseRepository: SCALicenseRepository,
    private val buildSCALicenseRepository: BuildSCALicenseRepository,
    private val licenseDetailRepository: LicenseDetailRepository
) : SCALicenseService {
    override fun getLicenseByTaskIdAndToolName(taskId: Long, toolName: String): List<SCALicenseEntity> =
        scaLicenseRepository.findByTaskIdAndToolName(taskId, toolName)

    override fun saveLicenses(taskId: Long, toolName: String, licenses: Collection<SCALicenseEntity>) {
        if (CollectionUtils.isEmpty(licenses)) {
            return
        }
        licenses.forEach {
            if (StringUtils.isEmpty(it.entityId)) {
                it.applyAuditInfoOnCreate()
            } else {
                it.applyAuditInfoOnUpdate()
            }
        }
        scaLicenseRepository.saveAll(licenses)
    }

    override fun getNewLicenseByTaskIdAndToolName(taskId: Long, toolName: String): List<SCALicenseEntity> =
        scaLicenseRepository.findByTaskIdAndToolNameAndStatusAndHasEnabledPackage(
            taskId,
            toolName,
            DefectStatus.NEW.value(),
            true
        )

    override fun saveBuildLicenses(
        taskId: Long,
        toolName: String,
        buildId: String,
        buildEntity: BuildEntity?,
        licenses: Collection<SCALicenseEntity>
    ) {
        if (licenses.isEmpty()) {
            return
        }
        val buildLicenseList = licenses.map {
            BuildSCALicenseEntity(taskId, toolName, buildId, buildEntity?.buildNo, it.entityId)
        }.toList()
        buildLicenseList.forEach { it.applyAuditInfoOnCreate() }
        val count = buildSCALicenseRepository.countByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId)
        if (count > 0) {
            buildSCALicenseRepository.removeByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId)
        }
        buildSCALicenseRepository.insert(buildLicenseList)
    }

    override fun getNewLicensesFromSnapshot(taskId: Long, toolName: String, buildId: String): List<SCALicenseEntity> {
        val buildLicenses = buildSCALicenseRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId)
        if (buildLicenses.isEmpty()) {
            return emptyList()
        }
        val licenseIds = buildLicenses.map { it.id }
        // 证书数量一般不多，可以不需要分页
        return scaLicenseRepository.findAllById(licenseIds).filter { it.status == DefectStatus.NEW.value() }
    }

    override fun uploadLicenses(licenses: List<LicenseDetailEntity>) {
        val names = licenses.map { it.name }
        val oldLicenseDetails = licenseDetailRepository.findByNameIn(names)
        val nameToOldEntity = oldLicenseDetails.associateBy { it.name }
        val updateLicenses = mutableListOf<LicenseDetailEntity>()
        for (license in licenses) {
            val oldLicense = nameToOldEntity[license.name] ?: license
            if (!oldLicense.entityId.isNullOrEmpty()) {
                updateLicenseInfo(license, oldLicense)
                oldLicense.applyAuditInfoOnUpdate()
            } else {
                oldLicense.alias = listOf(oldLicense.name.lowercase(), oldLicense.name.uppercase())
                oldLicense.applyAuditInfoOnCreate()
            }
            updateLicenses.add(oldLicense)
        }
        licenseDetailRepository.saveAll(updateLicenses)
    }

    private fun updateLicenseInfo(newLicense: LicenseDetailEntity, oldLicense: LicenseDetailEntity) {
        with(newLicense) {
            // 仅更新基本信息
            oldLicense.fullName = fullName
            oldLicense.summary = summary
            oldLicense.urls = urls
            oldLicense.gplCompatible = gplCompatible
            oldLicense.gplDesc = gplDesc
            oldLicense.osi = osi
            oldLicense.fsf = fsf
            oldLicense.spdx = spdx
            oldLicense.status = status
        }
    }
}
