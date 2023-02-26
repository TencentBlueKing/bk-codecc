package com.tencent.bk.codecc.defect.service.impl.handler

import com.tencent.bk.codecc.defect.pojo.HandlerDTO
import com.tencent.bk.codecc.defect.service.IHandler
import com.tencent.bk.codecc.defect.service.impl.CalTaskStatusServiceImpl
import com.tencent.devops.common.constant.ComConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 获取该次扫描的整体执行状态
 */
@Service
class CalTaskStatusHandler @Autowired constructor(
    private val handler: CodeScoringHandler,
    private val calTaskStatusServiceImpl: CalTaskStatusServiceImpl
) : IHandler {
    companion object {
        private val logger = LoggerFactory.getLogger(CalTaskStatusHandler::class.java)
    }

    override fun handler(handlerDTO: HandlerDTO) {
        try {
            calTaskStatusServiceImpl.getTaskStatus(handlerDTO)
        } catch (e: Throwable) {
            logger.error(
                "cal task status fail! ${handlerDTO.taskId}, ${handlerDTO.toolName}, ${handlerDTO.buildId}", e
            )
            handlerDTO.scanStatus = ComConstants.ScanStatus.FAIL
        } finally {
            // 代码算分以及维度信息统计，均依赖该成功状态
            handler.handler(handlerDTO)
        }
    }
}
