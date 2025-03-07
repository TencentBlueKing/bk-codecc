package com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca

import com.tencent.bk.codecc.defect.model.sca.BuildSCASbomPackageEntity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface BuildSCASbomPackageRepository : MongoRepository<BuildSCASbomPackageEntity, String> {

    fun findByTaskIdAndToolNameInAndBuildId(
        taskId: Long,
        toolName: Set<String>,
        buildIds: String
    ): List<BuildSCASbomPackageEntity>
}
