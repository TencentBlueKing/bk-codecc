package com.tencent.bk.codecc.defect.component

import com.tencent.bk.codecc.defect.dto.ScanTaskTriggerDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ExpireTaskHandleComponent @Autowired constructor(
    private val finishTaskHandleComponent: FinishTaskHandleComponent
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ExpireTaskHandleComponent::class.java)
    }

    fun updateExpiredTaskStatus(scanTaskTriggerDTO: ScanTaskTriggerDTO) {
        logger.info("start to handle expired task:${scanTaskTriggerDTO.taskId} buildId:${scanTaskTriggerDTO.buildId}")
        finishTaskHandleComponent.handleProcessingToFinish(
            scanTaskTriggerDTO.taskId,
            scanTaskTriggerDTO.buildId, "任务超时失败"
        )
        logger.info("end to handle expired task:${scanTaskTriggerDTO.taskId} buildId:${scanTaskTriggerDTO.buildId}")
    }
}
