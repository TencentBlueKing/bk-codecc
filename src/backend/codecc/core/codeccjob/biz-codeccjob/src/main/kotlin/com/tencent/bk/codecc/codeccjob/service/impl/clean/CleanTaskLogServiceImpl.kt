package com.tencent.bk.codecc.codeccjob.service.impl.clean

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.TaskLogRepository
import com.tencent.bk.codecc.codeccjob.service.ICleanMongoDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CleanTaskLogServiceImpl @Autowired constructor(
    private val taskLogRepository: TaskLogRepository
) : ICleanMongoDataService {

    override fun clean(projectId: String, taskId: Long, obsoleteBuildIdList: List<String>, taskToolList: List<String>) {
        val start = System.currentTimeMillis()
        taskLogRepository.deleteAllByTaskIdAndToolNameInAndBuildIdIn(taskId, taskToolList, obsoleteBuildIdList)
        logger.info("Clean task log: $taskId ${obsoleteBuildIdList.size} ${System.currentTimeMillis() - start}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CleanTaskLogServiceImpl::class.java)
    }
}
