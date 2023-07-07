package com.tencent.bk.codecc.defect.service.impl.handler

import com.tencent.bk.codecc.defect.pojo.HandlerDTO
import com.tencent.bk.codecc.defect.service.BuildSnapshotService
import com.tencent.bk.codecc.defect.service.IHandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 保存快照概要信息
 */
@Component
class SaveSnapshotSummaryHandler @Autowired constructor(
    private val buildSnapshotService: BuildSnapshotService,
    private val calTaskStatusHandler: CalTaskStatusHandler
) : IHandler {
    companion object {
        private val logger = LoggerFactory.getLogger(SaveSnapshotSummaryHandler::class.java)
    }

    override fun handler(handlerDTO: HandlerDTO) {
        try {
            buildSnapshotService.saveBuildSnapshotSummary(handlerDTO)
        } catch (e: Throwable) {
            logger.error(
                "save build defect summary fail, task: ${handlerDTO.taskId}, build: ${handlerDTO.buildId}, " +
                        "tool: ${handlerDTO.toolName}", e
            )
        } finally {
            calTaskStatusHandler.handler(handlerDTO)
        }
    }
}