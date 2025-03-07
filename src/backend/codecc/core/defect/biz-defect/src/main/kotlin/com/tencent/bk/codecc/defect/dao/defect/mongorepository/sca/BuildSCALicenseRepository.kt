package com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca

import com.tencent.bk.codecc.defect.model.sca.BuildSCALicenseEntity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface BuildSCALicenseRepository : MongoRepository<BuildSCALicenseEntity, String> {

    /**
     * 统计指定构建的SCA许可证数量
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param buildId 构建ID
     * @return 匹配的许可证数量
     */
    fun countByTaskIdAndToolNameAndBuildId(taskId: Long, toolName: String, buildId: String): Long

    /**
     * 查询指定构建的SCA许可证列表
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param buildId 构建ID
     * @return 匹配的许可证实体列表
     */
    fun findByTaskIdAndToolNameAndBuildId(taskId: Long, toolName: String, buildId: String): List<BuildSCALicenseEntity>

    /**
     * 根据多个工具查询指定构建的SCA许可证
     * @param taskId 任务ID
     * @param toolName 工具名称集合
     * @param buildIds 构建ID
     * @return 匹配的许可证实体列表
     */
    fun findByTaskIdAndToolNameInAndBuildId(
        taskId: Long,
        toolName: Set<String>,
        buildIds: String
    ): List<BuildSCALicenseEntity>

    /**
     * 删除指定构建的SCA许可证记录
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param buildId 构建ID
     */
    fun removeByTaskIdAndToolNameAndBuildId(taskId: Long, toolName: String, buildId: String)
}
