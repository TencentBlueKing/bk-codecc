package com.tencent.bk.codecc.defect.component

import com.tencent.bk.codecc.task.vo.PluginErrorVO
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 插件错误上报处理，结束扫描。
 */
@Component
class PluginErrorHandleComponent @Autowired constructor(
    private val finishTaskHandleComponent: FinishTaskHandleComponent,
) {

    fun handlePluginErrorCallback(pluginErrorVO: PluginErrorVO) {
        val taskId = pluginErrorVO.taskId
        val buildId = pluginErrorVO.buildId
        try {
            logger.info("start to handle plugin error taskId:$taskId buildId:$buildId")
            if (taskId == null || StringUtils.isBlank(buildId)) {
                return
            }
            finishTaskHandleComponent.handleProcessingToFinish(taskId, buildId, "任务失败")
            logger.info("end to handle handle plugin error taskId:$taskId buildId:$buildId")
        } catch (e: Exception) {
            logger.error(
                "handle handle plugin error taskId:$taskId , buildId: $buildId,", e
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PluginErrorHandleComponent::class.java)
    }
}
