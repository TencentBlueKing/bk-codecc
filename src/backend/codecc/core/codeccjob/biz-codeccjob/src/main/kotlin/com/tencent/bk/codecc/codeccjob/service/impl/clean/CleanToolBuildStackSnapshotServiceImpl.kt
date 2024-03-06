package com.tencent.bk.codecc.codeccjob.service.impl.clean

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.SnapShotRepository
import com.tencent.bk.codecc.codeccjob.service.ICleanMongoDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CleanToolBuildStackSnapshotServiceImpl @Autowired constructor(
    private val snapShotRepository: SnapShotRepository
) : ICleanMongoDataService {

    override fun clean(projectId: String, taskId: Long, obsoleteBuildIdList: List<String>, taskToolList: List<String>) {
        val start = System.currentTimeMillis()
        snapShotRepository.deleteAllByProjectIdAndBuildIdInAndTaskId(projectId, obsoleteBuildIdList, taskId)
        logger.info(
            "Clean tool build stack snapshot:" +
                    " $projectId $taskId ${obsoleteBuildIdList.size} ${System.currentTimeMillis() - start}"
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CleanToolBuildStackSnapshotServiceImpl::class.java)
    }
}
