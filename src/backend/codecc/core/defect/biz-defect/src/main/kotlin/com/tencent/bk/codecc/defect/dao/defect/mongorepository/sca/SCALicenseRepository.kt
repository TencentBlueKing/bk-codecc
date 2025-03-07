package com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca

import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
/**
 * SCA许可证数据仓库接口
 */
@Repository
interface SCALicenseRepository : MongoRepository<SCALicenseEntity, String> {
    /**
     * 根据任务ID和工具名称查询许可证列表
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @return 匹配的许可证实体列表
     */
    fun findByTaskIdAndToolName(
        taskId: Long,
        toolName: String
    ): List<SCALicenseEntity>

    /**
     * 根据任务ID、工具名称、状态和有效包标记查询许可证
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param status 许可证状态
     * @param hasEnabledPackage 是否包含有效组件包
     * @return 匹配的许可证实体列表
     */
    fun findByTaskIdAndToolNameAndStatusAndHasEnabledPackage(
        taskId: Long,
        toolName: String,
        status: Int,
        hasEnabledPackage: Boolean
    ): List<SCALicenseEntity>

    /**
     * 根据任务ID、工具名称和许可证名称集合查询
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param names 许可证名称集合
     * @return 匹配的许可证实体列表
     */
    fun findByTaskIdAndToolNameAndNameIn(
        taskId: Long,
        toolName: String,
        names: List<String>
    ): List<SCALicenseEntity>

    /**
     * 根据任务ID集合和实体ID集合批量查询
     * @param taskId 任务ID集合
     * @param entityId 实体ID集合
     * @return 匹配的许可证实体列表
     */
    fun findByTaskIdInAndEntityIdIn(
        taskId: List<Long>,
        entityId: Set<String>
    ): List<SCALicenseEntity>
}
