package com.tencent.bk.codecc.codeccjob.service.impl.clean

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.BgCheckRepository
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.FluctuationTaskCheckRepository
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.LanguageCheckRepository
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.ProjectCheckRepository
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.TaskCheckRepository
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.ToolCheckRepository
import com.tencent.bk.codecc.codeccjob.service.ICleanMongoDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CleanOpenSourceReportServiceImpl @Autowired constructor(
    private val languageCheckRepository: LanguageCheckRepository,
    private val projectCheckRepository: ProjectCheckRepository,
    private val taskCheckRepository: TaskCheckRepository,
    private val toolCheckRepository: ToolCheckRepository,
    private val bgCheckRepository: BgCheckRepository,
    private val fluctuationTaskCheckRepository: FluctuationTaskCheckRepository
) : ICleanMongoDataService {
    override fun clean(projectId: String, taskId: Long, obsoleteBuildIdList: List<String>, taskToolList: List<String>) {
        val start = System.currentTimeMillis()
        // 定时清理开源扫描报告的上传记录
        languageCheckRepository.deleteAllByBuildIdIn(obsoleteBuildIdList)
        projectCheckRepository.deleteAllByBuildIdIn(obsoleteBuildIdList)
        taskCheckRepository.deleteAllByBuildIdIn(obsoleteBuildIdList)
        toolCheckRepository.deleteAllByBuildIdIn(obsoleteBuildIdList)
        bgCheckRepository.deleteAllByBuildIdIn(obsoleteBuildIdList)
        fluctuationTaskCheckRepository.deleteAllByTaskId(taskId)
        logger.info("Clean language check: $taskId ${obsoleteBuildIdList.size} ${System.currentTimeMillis() - start}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CleanOpenSourceReportServiceImpl::class.java)
    }
}
