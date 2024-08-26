package com.tencent.bk.codecc.codeccjob.service.impl.clean

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.TaskInvalidToolDefectLogRepository
import com.tencent.bk.codecc.codeccjob.service.ICleanMongoDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CleanTaskInvalidToolDefectServiceImpl @Autowired constructor(
    private val taskInvalidToolDefectLogRepository: TaskInvalidToolDefectLogRepository
) : ICleanMongoDataService {

    override fun clean(projectId: String, taskId: Long, obsoleteBuildIdList: List<String>, taskToolList: List<String>) {
        val start = System.currentTimeMillis()
        taskInvalidToolDefectLogRepository.deleteAllByTaskIdAndBuildIdIn(taskId, obsoleteBuildIdList)
        logger.info(
            "Clean Task Invalid Tool Defect Log: $taskId ${obsoleteBuildIdList.size}" +
                    " ${System.currentTimeMillis() - start}"
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CleanTaskInvalidToolDefectServiceImpl::class.java)
    }
}
