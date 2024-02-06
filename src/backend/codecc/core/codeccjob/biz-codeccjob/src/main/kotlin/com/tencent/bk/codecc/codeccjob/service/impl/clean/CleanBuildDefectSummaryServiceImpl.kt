package com.tencent.bk.codecc.codeccjob.service.impl.clean

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.BuildDefectSummaryRepository
import com.tencent.bk.codecc.codeccjob.service.ICleanMongoDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CleanBuildDefectSummaryServiceImpl @Autowired constructor(
    private val buildDefectSummaryRepository: BuildDefectSummaryRepository
) : ICleanMongoDataService {

    override fun clean(projectId: String, taskId: Long, obsoleteBuildIdList: List<String>, taskToolList: List<String>) {
        val start = System.currentTimeMillis()
        buildDefectSummaryRepository.deleteAllByTaskIdAndBuildIdIn(taskId, obsoleteBuildIdList)
        logger.info(
            "Clean Build Defect Summary: " +
                    "$taskId ${obsoleteBuildIdList.size} ${System.currentTimeMillis() - start}"
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CleanBuildDefectSummaryServiceImpl::class.java)
    }
}
