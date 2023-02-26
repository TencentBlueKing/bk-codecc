package com.tencent.bk.codecc.defect.component

import com.tencent.bk.codecc.defect.api.ServiceReportTaskLogRestResource
import com.tencent.bk.codecc.defect.dao.mongotemplate.TaskLogDao
import com.tencent.bk.codecc.defect.dao.mongotemplate.TaskLogOverviewDao
import com.tencent.bk.codecc.defect.model.TaskLogEntity
import com.tencent.bk.codecc.defect.service.impl.ScanFinishEventService
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class FinishTaskHandleComponent @Autowired constructor(
    private val taskLogDao: TaskLogDao,
    private val taskLogOverviewDao: TaskLogOverviewDao,
    private val client: Client,
    private val scanFinishEventService: ScanFinishEventService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(FinishTaskHandleComponent::class.java)
    }

    fun handleProcessingToFinish(taskId: Long, buildId: String, errorMsg: String) {
        try {
            logger.info("start to handle processing task:${taskId} buildId:${buildId}")
            //先查询TaskOverview,获取工具配置
            val overview =
                taskLogOverviewDao.findOneByTaskIdAndBuildId(taskId, buildId)
            //按构建id查询任务记录清单
            val taskLogList = taskLogDao.findFirstByTaskIdAndBuildIdOrderbyStartTime(
                taskId,
                buildId
            )
            if (overview == null && taskLogList.isNullOrEmpty()) {
                //没有记录，返回
                logger.info("task:${taskId} buildId:${buildId}  overview and task log is empty")
                return
            }
            //如果TaskOverview不为空,且为进行中状态
            if (overview != null && overview.status != ComConstants.ScanStatus.SUCCESS.code) {
                //判断Overview的状态，如果TaskLog全部成功，那么它应该为成功，否则置为失败
                val overviewStatus = judgeOverviewStatus(overview.toolList, taskLogList)
                if (overview.status != overviewStatus) {
                    taskLogOverviewDao.updateStatus(taskId, buildId, overviewStatus)
                }
            }
            //更新TaskLog状态
            updateTaskLog(taskId, buildId, overview.toolList, taskLogList, errorMsg)

            //通知扫描结束
            scanFinishEventService.sendScanFinishEvent(taskId, buildId)
        } catch (e: Exception) {
            logger.error("handle processing task fail! task id: $taskId", e)
        }
    }

    /**
     * 判断Overview的状态
     * 如果工具与TaskLog均符合，且TaskLog都为成功，则设为已完成
     * 否则置为失败
     */
    private fun judgeOverviewStatus(toolList: List<String>, taskLogList: List<TaskLogEntity>?): Int {
        if (toolList.isNullOrEmpty() || taskLogList.isNullOrEmpty()) {
            return ComConstants.ScanStatus.FAIL.code
        }
        val toolAndFlagMap = taskLogList.map {
            it.toolName to it.flag
        }.toMap()
        toolList.forEach {
            val flag = toolAndFlagMap[it]
            if (flag == null || flag != ComConstants.StepFlag.SUCC.value()) {
                return ComConstants.ScanStatus.FAIL.code
            }
        }
        return ComConstants.ScanStatus.SUCCESS.code
    }



    private fun updateTaskLog(
        taskId: Long, buildId: String,
        toolList: List<String>, taskLogList: List<TaskLogEntity>?,
        errorMsg: String
    ) {
        val mutableList = mutableListOf<String>()
        if (!toolList.isNullOrEmpty()) {
            mutableList.addAll(toolList)
        }
        if (!taskLogList.isNullOrEmpty()) {
            //如果overview状态已清晰，那么检查一下，taskLogOverview的状态是否正确
            taskLogList.forEach {
                if (!mutableList.isNullOrEmpty() && mutableList.contains(it.toolName)) {
                    mutableList.remove(it.toolName)
                }
                //如果超时状态下，还为处理中状态，则进行更新
                if (it.flag == ComConstants.StepFlag.PROCESSING.value()) {
                    val uploadTaskLogStepVO = UploadTaskLogStepVO()
                    uploadTaskLogStepVO.taskId = it.taskId
                    uploadTaskLogStepVO.streamName = it.streamName
                    uploadTaskLogStepVO.toolName = it.toolName
                    uploadTaskLogStepVO.startTime = 0L
                    uploadTaskLogStepVO.endTime = System.currentTimeMillis()
                    uploadTaskLogStepVO.flag = ComConstants.StepFlag.FAIL.value()
                    uploadTaskLogStepVO.msg = errorMsg
                    uploadTaskLogStepVO.stepNum = it.currStep
                    uploadTaskLogStepVO.pipelineBuildId = it.buildId
                    uploadTaskLogStepVO.triggerFrom = it.triggerFrom
                    client.get(ServiceReportTaskLogRestResource::class.java).uploadTaskLog(uploadTaskLogStepVO)
                }
            }
        }
        //补偿未运行的TaskLog
        if (!mutableList.isNullOrEmpty()) {
            mutableList.forEach {
                val uploadTaskLogStepVO = UploadTaskLogStepVO()
                uploadTaskLogStepVO.taskId = taskId
                uploadTaskLogStepVO.streamName = ""
                uploadTaskLogStepVO.toolName = it
                uploadTaskLogStepVO.startTime = 0L
                uploadTaskLogStepVO.endTime = System.currentTimeMillis()
                uploadTaskLogStepVO.flag = ComConstants.StepFlag.FAIL.value()
                uploadTaskLogStepVO.msg = errorMsg
                uploadTaskLogStepVO.stepNum = 1
                uploadTaskLogStepVO.pipelineBuildId = buildId
                uploadTaskLogStepVO.triggerFrom = ""
                client.get(ServiceReportTaskLogRestResource::class.java).uploadTaskLog(uploadTaskLogStepVO)
            }
        }
    }
}
