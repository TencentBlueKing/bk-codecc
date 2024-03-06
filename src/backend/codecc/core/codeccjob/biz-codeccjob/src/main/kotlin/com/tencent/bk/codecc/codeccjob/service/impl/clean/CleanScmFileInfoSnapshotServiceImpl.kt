package com.tencent.bk.codecc.codeccjob.service.impl.clean

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.ScmFileInfoSnapshotRepository
import com.tencent.bk.codecc.codeccjob.service.ICleanMongoDataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CleanScmFileInfoSnapshotServiceImpl @Autowired constructor(
    private val scmFileInfoSnapshotRepository: ScmFileInfoSnapshotRepository
) : ICleanMongoDataService {
    override fun clean(projectId: String, taskId: Long, obsoleteBuildIdList: List<String>, taskToolList: List<String>) {
        scmFileInfoSnapshotRepository.deleteAllByTaskIdAndBuildIdIn(taskId, obsoleteBuildIdList)
    }
}
