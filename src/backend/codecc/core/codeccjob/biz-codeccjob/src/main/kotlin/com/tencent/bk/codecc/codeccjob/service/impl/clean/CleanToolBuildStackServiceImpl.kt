package com.tencent.bk.codecc.codeccjob.service.impl.clean

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.ToolBuildStackRepository
import com.tencent.bk.codecc.codeccjob.service.ICleanMongoDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CleanToolBuildStackServiceImpl @Autowired constructor(
    private val toolBuildStackRepository: ToolBuildStackRepository
) : ICleanMongoDataService {

    override fun clean(projectId: String, taskId: Long, obsoleteBuildIdList: List<String>, taskToolList: List<String>) {
        val start = System.currentTimeMillis()
        toolBuildStackRepository.deleteAllByTaskIdAndToolNameInAndBuildIdIn(taskId, taskToolList, obsoleteBuildIdList)
        logger.info("Clean tool build stack: $taskId ${obsoleteBuildIdList.size} ${System.currentTimeMillis() - start}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CleanToolBuildStackServiceImpl::class.java)
    }
}
