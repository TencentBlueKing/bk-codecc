package com.tencent.bk.codecc.defect.dao.defect.mongorepository.statistic

import com.tencent.bk.codecc.defect.model.statistic.SCAStatisticEntity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface SCAStatisticRepository : MongoRepository<SCAStatisticEntity, String> {

    /**
     * 根据任务ID、工具名称和构建ID查询最新的统计记录
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param buildId 构建ID
     * @return 匹配的统计实体或null
     */
    fun findFirstByTaskIdAndToolNameAndBuildId(taskId: Long, toolName: String, buildId: String): SCAStatisticEntity?

    /**
     * 根据任务ID和构建ID查询最新的统计记录
     * @param taskId 任务ID
     * @param buildId 构建ID
     * @return 匹配的统计实体或null
     */
    fun findFirstByTaskIdAndBuildId(taskId: Long, buildId: String): SCAStatisticEntity?

    /**
     * 根据任务ID查询最新的统计记录（按时间倒序）
     * @param taskId 任务ID
     * @return 最新的统计实体或null
     */
    fun findTopByTaskIdOrderByTimeDesc(taskId: Long): SCAStatisticEntity?

    /**
     * 根据任务ID和工具名称查询最新的统计记录（按时间倒序）
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @return 最新的统计实体或null
     */
    fun findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId: Long, toolName: String): SCAStatisticEntity?
}
