package com.tencent.bk.codecc.defect.service.impl.handler

import com.tencent.bk.codecc.defect.pojo.HandlerDTO
import com.tencent.bk.codecc.defect.service.IHandler
import com.tencent.bk.codecc.defect.service.impl.ClusterDefectServiceImpl
import com.tencent.devops.common.constant.ComConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 按维度统计告警信息
 */
@Service
class ClusterHandler @Autowired constructor(
    private val clusterDefectServiceImpl: ClusterDefectServiceImpl
) : IHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(ClusterHandler::class.java)
    }

    override fun handler(handlerDTO: HandlerDTO) {
        try {
            if (handlerDTO.scanStatus == ComConstants.ScanStatus.SUCCESS) {
                clusterDefectServiceImpl.cluster(
                    taskId = handlerDTO.taskId,
                    buildId = handlerDTO.buildId,
                    toolName = handlerDTO.toolName
                )
            }
        } catch (e: Throwable) {
            logger.error(
                "cluster defect fail! ${handlerDTO.taskId}, ${handlerDTO.toolName}, ${handlerDTO.buildId}", e
            )
        }
    }
}
