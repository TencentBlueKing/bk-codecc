package com.tencent.bk.codecc.defect.service.sca

import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.sca.LicenseDetailEntity
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity

interface SCALicenseService {

    /**
     * 根据任务ID和工具名称获取许可证列表
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @return 匹配的SCA许可证实体列表
     */
    fun getLicenseByTaskIdAndToolName(taskId: Long, toolName: String): List<SCALicenseEntity>

    /**
     * 保存许可证信息集合
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param licenses 需要保存的许可证集合
     */
    fun saveLicenses(taskId: Long, toolName: String, licenses: Collection<SCALicenseEntity>)

    /**
     * 获取新发现的许可证列表
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @return 新发现的许可证实体列表
     */
    fun getNewLicenseByTaskIdAndToolName(taskId: Long, toolName: String): List<SCALicenseEntity>

    /**
     * 保存构建关联的许可证信息
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param buildId 构建ID
     * @param buildEntity 构建实体信息（可为空）
     * @param licenses 需要保存的许可证集合
     */
    fun saveBuildLicenses(
        taskId: Long,
        toolName: String,
        buildId: String,
        buildEntity: BuildEntity?,
        licenses: Collection<SCALicenseEntity>
    )

    /**
     * 从构建快照中获取新发现的许可证
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param buildId 构建ID
     * @return 新发现的许可证列表
     */
    fun getNewLicensesFromSnapshot(
        taskId: Long,
        toolName: String,
        buildId: String
    ): List<SCALicenseEntity>

    /**
     * 上传许可证详细信息
     * @param licenses 需要上传的许可证详情实体列表
     */
    fun uploadLicenses(licenses: List<LicenseDetailEntity>)
}
