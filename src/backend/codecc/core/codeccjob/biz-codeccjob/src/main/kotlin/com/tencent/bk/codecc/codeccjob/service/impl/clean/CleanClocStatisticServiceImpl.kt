package com.tencent.bk.codecc.codeccjob.service.impl.clean

import com.tencent.bk.codecc.codeccjob.dao.mongorepository.CLOCStatisticRepository
import com.tencent.bk.codecc.codeccjob.service.ICleanMongoDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CleanClocStatisticServiceImpl @Autowired constructor(
    private val clocStatisticRepository: CLOCStatisticRepository
): ICleanMongoDataService {
    override fun clean(projectId: String, taskId: Long, obsoleteBuildIdList: List<String>, taskToolList: List<String>) {
        val start = System.currentTimeMillis()
        clocStatisticRepository.deleteAllByTaskIdAndToolNameInAndBuildIdIn(taskId, taskToolList, obsoleteBuildIdList)
        logger.info("Clean cloc statistic: $taskId ${obsoleteBuildIdList.size} ${System.currentTimeMillis() - start}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CleanClocStatisticServiceImpl::class.java)
    }
}