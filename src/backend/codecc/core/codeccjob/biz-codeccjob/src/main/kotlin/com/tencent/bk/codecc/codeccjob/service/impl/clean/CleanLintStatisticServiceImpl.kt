package com.tencent.bk.codecc.codeccjob.service.impl.clean

import com.tencent.bk.codecc.codeccjob.dao.mongorepository.LintStatisticRepository
import com.tencent.bk.codecc.codeccjob.service.ICleanMongoDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CleanLintStatisticServiceImpl @Autowired constructor(
    private val lintStatisticRepository: LintStatisticRepository
): ICleanMongoDataService {
    override fun clean(projectId: String, taskId: Long, obsoleteBuildIdList: List<String>, taskToolList: List<String>) {
        val start = System.currentTimeMillis()
        lintStatisticRepository.deleteAllByTaskIdAndToolNameInAndBuildIdIn(taskId, taskToolList, obsoleteBuildIdList)
        logger.info("Clean lint statistic: $taskId ${obsoleteBuildIdList.size} ${System.currentTimeMillis() - start}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CleanLintStatisticServiceImpl::class.java)
    }
}
