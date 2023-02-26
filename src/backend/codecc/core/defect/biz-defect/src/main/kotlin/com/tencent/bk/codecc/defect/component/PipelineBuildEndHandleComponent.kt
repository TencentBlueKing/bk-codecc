package com.tencent.bk.codecc.defect.component

import com.alibaba.fastjson.JSONObject
import com.tencent.bk.codecc.defect.dao.mongotemplate.TaskLogOverviewDao
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.bk.codecc.task.vo.PipelineCallbackVo
import com.tencent.devops.common.client.Client
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PipelineBuildEndHandleComponent @Autowired constructor(
    private val finishTaskHandleComponent: FinishTaskHandleComponent,
    private val taskLogOverviewDao: TaskLogOverviewDao,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildEndHandleComponent::class.java)
    }

    fun handlePipelineBuildEndCallback(pipelineCallbackVo: PipelineCallbackVo) {
        val pipelineId = pipelineCallbackVo.data?.pipelineId
        val buildId = pipelineCallbackVo.data?.buildId
        val userId = pipelineCallbackVo.data?.userId
        try {
            logger.info("start to handle pipeline build end pipeline:${pipelineId} buildId:${buildId}")
            //获取所有的流水线任务
            val allTaskResult =
                client.get(ServiceTaskRestResource::class.java).getPipelineAllTaskId(pipelineId, userId)
            if (allTaskResult.isNotOk()) {
                logger.error(
                    "handle pipeline build end get all task fail! " +
                            "pipelineId : $pipelineId, result: ${JSONObject.toJSONString(allTaskResult)}."
                )
                return
            }
            if (allTaskResult.data.isNullOrEmpty()) {
                logger.info("handle pipeline build end get all task empty! pipelineId : $pipelineId.")
                return
            }
            val overviews = taskLogOverviewDao.findByTaskIdsAndBuildId(allTaskResult.data, buildId!!)
            overviews.forEach { overview ->
                finishTaskHandleComponent.handleProcessingToFinish(overview.taskId,
                    overview.buildId, "任务失败")
            }
            logger.info("end to handle pipeline build end pipeline:${pipelineId} buildId:${buildId}")
        } catch (e: Exception) {
            logger.error(
                "handle pipeline build end fail! pipeline id: $pipelineId, buildId: $buildId," +
                        "userId: $userId", e
            )
        }
    }
}
