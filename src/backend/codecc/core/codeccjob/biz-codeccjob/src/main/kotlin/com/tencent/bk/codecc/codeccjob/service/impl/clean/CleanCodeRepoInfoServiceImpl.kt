package com.tencent.bk.codecc.codeccjob.service.impl.clean

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CodeRepoInfoRepository
import com.tencent.bk.codecc.codeccjob.service.ICleanMongoDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CleanCodeRepoInfoServiceImpl @Autowired constructor(
    private val codeRepoInfoRepository: CodeRepoInfoRepository
) : ICleanMongoDataService {
    override fun clean(projectId: String, taskId: Long, obsoleteBuildIdList: List<String>, taskToolList: List<String>) {
        val beginTime = System.currentTimeMillis()
        codeRepoInfoRepository.deleteAllByTaskIdAndBuildIdIn(taskId, obsoleteBuildIdList)
        logger.info("clean code repo info: $taskId, ${obsoleteBuildIdList.size}, ${System.currentTimeMillis() - beginTime}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CleanCodeRepoInfoServiceImpl::class.java)
    }
}