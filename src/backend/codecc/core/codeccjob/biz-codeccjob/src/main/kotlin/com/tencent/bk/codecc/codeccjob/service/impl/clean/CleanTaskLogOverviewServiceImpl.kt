package com.tencent.bk.codecc.codeccjob.service.impl.clean

import com.tencent.bk.codecc.codeccjob.dao.mongorepository.TaskLogOverviewRepository
import com.tencent.bk.codecc.codeccjob.service.ICleanMongoDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CleanTaskLogOverviewServiceImpl @Autowired constructor(
    private val taskLogOverviewRepository: TaskLogOverviewRepository
): ICleanMongoDataService {

    override fun clean(projectId: String, taskId: Long, obsoleteBuildIdList: List<String>, taskToolList: List<String>) {
        val start = System.currentTimeMillis()
        taskLogOverviewRepository.deleteAllByTaskIdAndBuildIdIn(taskId, obsoleteBuildIdList)
        logger.info("Clean task log overview:" +
            " $taskId ${obsoleteBuildIdList.size} ${System.currentTimeMillis() - start}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CleanTaskLogOverviewServiceImpl::class.java)
    }
}
