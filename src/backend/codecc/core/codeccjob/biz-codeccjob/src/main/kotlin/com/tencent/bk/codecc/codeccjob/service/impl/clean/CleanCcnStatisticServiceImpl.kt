package com.tencent.bk.codecc.codeccjob.service.impl.clean

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CCNStatisticRepository
import com.tencent.bk.codecc.codeccjob.service.ICleanMongoDataService
import com.tencent.devops.common.constant.ComConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CleanCcnStatisticServiceImpl @Autowired constructor(
    private val ccnStatistcRepository: CCNStatisticRepository
) : ICleanMongoDataService {

    override fun clean(projectId: String, taskId: Long, obsoleteBuildIdList: List<String>, taskToolList: List<String>) {
        val start = System.currentTimeMillis()
        ccnStatistcRepository.deleteAllByTaskIdAndToolNameAndBuildIdIn(
            taskId, ComConstants.Tool.CCN.name, obsoleteBuildIdList
        )
        logger.info("Clean ccn statistic: $taskId ${obsoleteBuildIdList.size} ${System.currentTimeMillis() - start}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CleanCcnStatisticServiceImpl::class.java)
    }
}
