/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.model.SnapShotEntity
import com.tencent.bk.codecc.defect.model.TaskLogEntity
import com.tencent.bk.codecc.defect.service.AfterDevopsCallBackOpsService
import com.tencent.bk.codecc.defect.service.PipelineService
import com.tencent.bk.codecc.defect.service.SnapShotService
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServiceBuildResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
open class PipelineServiceImpl @Autowired constructor(
    private val client: Client,
    private val snapShotService: SnapShotService,
    private val taskLogOverviewService: TaskLogOverviewService,
    private val afterDevopsCallBackOpsService: AfterDevopsCallBackOpsService
) : PipelineService {

    @Async("asyncTaskExecutor")
    override fun handleDevopsCallBack(
        tasklog: TaskLogEntity,
        taskStep: TaskLogEntity.TaskUnit,
        toolName: String,
        taskDetailVO: TaskDetailVO
    ) {
        val taskId = tasklog.taskId
        val pipelineId = tasklog.pipelineId
        val buildId = tasklog.buildId
        if (pipelineId.isNullOrBlank() || buildId.isNullOrBlank()) {
            logger.info("pipeline id or build id of task[{}] is empty", taskId)
            return
        }

        val resultStatus = getResultStatus(taskStep, toolName)

        if (null == resultStatus) {
            logger.info("analyze task not finish yet, {}", taskId)
            return
        }

        // 保存告警快照
        val resultMessage = if (resultStatus != "success") taskStep.msg else ""
        val snapShotEntity = snapShotService.saveToolBuildSnapShot(
            taskId, taskDetailVO.projectId, taskDetailVO.pipelineId, buildId, resultStatus,
            resultMessage, toolName
        )

        val pair = taskLogOverviewService.getAutoScanLangFlagAndExeTools(taskId, buildId)
        val autoLangScanFlag = pair.first
        var effectiveTools = pair.second

        if (effectiveTools.isEmpty()) {
            effectiveTools = taskDetailVO.toolConfigInfoList.filter { toolConfigInfoVO ->
                toolConfigInfoVO.followStatus != ComConstants.FOLLOW_STATUS.WITHDRAW.value()
            }.map { toolConfigInfoVO ->
                toolConfigInfoVO.toolName
            }
        }

        // 如果接入的工具没有全部生成快照，就不需要发送给蓝盾
        if (!isAllToolsComplete(snapShotEntity, effectiveTools)) {
            logger.info("not all tool completed! build id is {}", buildId)
            return
        }

        // 自动识别语言扫描，不需要进行DevopsCallback
        if (autoLangScanFlag) {
            logger.info("auto lang scan ! build id is {}", buildId)
            return
        }

        // 所有工具完成后，进行处理
        afterDevopsCallBackOpsService.doAfterHandleDevopsCallBack(
            taskId, taskDetailVO, buildId,
            snapShotEntity, toolName
        )
    }

    override fun stopRunningTask(
        projectId: String,
        pipelineId: String,
        taskId: Long?,
        buildId: String,
        userName: String,
        nameEn: String
    ) {
        logger.info("execute pipeline task! task id: $taskId")
        if (projectId.isBlank() || pipelineId.isBlank() || null == taskId) {
            logger.error("task not exists! task id is: {}", taskId)
            throw CodeCCException(
                errorCode = CommonMessageCode.RECORD_NOT_EXITS,
                params = arrayOf("任务参数"),
                errorCause = null
            )
        }

        var channelCode = ChannelCode.CODECC_EE
        if (nameEn.startsWith(ComConstants.OLD_CODECC_ENNAME_PREFIX)) {
            channelCode = ChannelCode.CODECC
        }

        // 停止流水线
        val shutdownResult = client.getDevopsService(ServiceBuildResource::class.java).manualShutdown(
            userName, projectId, pipelineId, buildId, channelCode
        )
        if (shutdownResult.isNotOk() || null == shutdownResult.data || shutdownResult.data != true) {
            logger.error(
                "shut down pipeline fail! project id: {}, pipeline id: {}, build id: {}, msg: {}", projectId,
                pipelineId, buildId, shutdownResult.message
            )
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
//        updateTaskAbortStep(taskBaseVO.nameEn, toolName, taskLogVO, "任务被手动中断")
    }

    /**
     * 判断所有工具是否都已经生成快照
     */
    private fun isAllToolsComplete(snapShot: SnapShotEntity, effectiveTools: List<String>): Boolean {
        return effectiveTools.filterNot { effectiveTool ->
            snapShot.toolSnapshotList.any { toolSnapShotEntity ->
                toolSnapShotEntity.toolNameEn == effectiveTool
            }
        }.isNullOrEmpty()
    }

    /**
     * 获取结果状态
     */
    private fun getResultStatus(
        taskStep: TaskLogEntity.TaskUnit,
        toolName: String
    ): String? {
        val finishStep =
            if (ComConstants.Tool.COVERITY.name == toolName || ComConstants.Tool.KLOCWORK.name == toolName) {
                ComConstants.Step4Cov.DEFECT_SYNS.value()
            } else {
                ComConstants.Step4MutliTool.COMMIT.value()
            }
        return if (taskStep.flag == ComConstants.StepFlag.FAIL.value() || taskStep.flag == ComConstants.StepFlag.ABORT.value()) {
            ComConstants.RDMCoverityStatus.failed.name
        } else if (taskStep.flag == ComConstants.StepFlag.SUCC.value() && taskStep.endTime != 0L) {
            if (taskStep.stepNum == finishStep) {
                ComConstants.RDMCoverityStatus.success.name
            } else {
                null
            }
        } else {
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineServiceImpl::class.java)
    }
}
