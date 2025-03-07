package com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca

import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface SCASbomPackageRepository : MongoRepository<SCASbomPackageEntity, String> {
    fun findByTaskIdAndToolName(
        taskId: Long,
        toolName: String
    ): List<SCASbomPackageEntity>

    fun findByTaskIdInAndEntityIdIn(
        taskId: List<Long>,
        entityId: Set<String>
    ): List<SCASbomPackageEntity>
}
