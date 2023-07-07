/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Lists
import com.tencent.bk.codecc.defect.model.SnapShotEntity
import com.tencent.bk.codecc.defect.model.TaskLogEntity
import com.tencent.bk.codecc.defect.service.PipelineAfterCallBackOpsService
import com.tencent.bk.codecc.defect.service.PipelineService
import com.tencent.bk.codecc.defect.service.SnapShotService
import com.tencent.bk.codecc.defect.service.impl.redline.CompileRedLineReportServiceImpl
import com.tencent.bk.codecc.defect.service.impl.redline.RedLineReportServiceImpl
import com.tencent.bk.codecc.defect.vo.TaskPersonalStatisticRefreshReq
import com.tencent.bk.codecc.task.api.ServiceToolRestResource
import com.tencent.bk.codecc.task.enums.EmailType
import com.tencent.bk.codecc.task.pojo.EmailNotifyModel
import com.tencent.bk.codecc.task.pojo.RtxNotifyModel
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.api.ToolMetaBaseVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.proxy.DevopsProxy
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.lock.RedisLock
import com.tencent.devops.common.service.ToolMetaCacheService
import com.tencent.devops.common.web.mq.EXCHANGE_CODECC_GENERAL_NOTIFY
import com.tencent.devops.common.web.mq.EXCHANGE_TASK_PERSONAL
import com.tencent.devops.common.web.mq.ROUTE_CODECC_EMAIL_NOTIFY
import com.tencent.devops.common.web.mq.ROUTE_CODECC_RTX_NOTIFY
import com.tencent.devops.common.web.mq.ROUTE_TASK_PERSONAL
import com.tencent.devops.plugin.api.ExternalCodeccResource
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.quality.api.v2.ExternalQualityResource
import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import com.tencent.devops.quality.api.v2.pojo.request.MetadataCallback
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
open class PipelineServiceImpl @Autowired constructor(
    private val client: Client,
    private val snapShotService: SnapShotService,
    private val redLineReportServiceImpl: RedLineReportServiceImpl,
    private val rabbitTemplate: RabbitTemplate,
    private val objectMapper: ObjectMapper,
    private val taskLogOverviewServiceImpl: TaskLogOverviewServiceImpl,
    private val redisTemplate: RedisTemplate<String, String>,
    private var compileRedLineReportServiceImpl: CompileRedLineReportServiceImpl,
    private var toolMetaCache: ToolMetaCacheService,
    private val pipelineAfterCallBackOpsService : PipelineAfterCallBackOpsService
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

        var effectiveTools = taskLogOverviewServiceImpl.getActualExeTools(taskId, buildId)

        if (effectiveTools.isNullOrEmpty()) {
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

        logger.info("all tool completed! ready to send report! build id is {}", buildId)

        // 流水线创建的，则处理产出报告以及红线
        if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() == taskDetailVO.createFrom) {
            val toolOrderResult = client.get(ServiceToolRestResource::class.java).findToolOrder()
            if (toolOrderResult.isOk() && null != toolOrderResult.data) {
                val toolOrder = toolOrderResult.data!!.split(",")
                snapShotEntity.toolSnapshotList.sortBy {
                    if (toolOrder.contains(it.toolNameEn)) {
                        toolOrder.indexOf(it.toolNameEn)
                    } else {
                        Integer.MAX_VALUE
                    }
                }
            }

            // TODO 为了下架coverity工具后不会导致配制了coverity指标的质量红线被拦截，这里做个兼容，给coverity的指标都上报0
            var hasCoverity = taskDetailVO.toolConfigInfoList.filter { toolConfigInfoVO ->
                toolConfigInfoVO.followStatus != ComConstants.FOLLOW_STATUS.WITHDRAW.value()
            }.any { toolConfigInfoVO ->
                toolConfigInfoVO.toolName == ComConstants.Tool.COVERITY.name
            }
            if (hasCoverity) {
                val toolMetaBase: ToolMetaBaseVO = toolMetaCache.getToolBaseMetaCache(ComConstants.Tool.COVERITY.name)
                if (ComConstants.ToolIntegratedStatus.D.name == toolMetaBase.status) {
                    logger.info("tool was removed: {}, {}", ComConstants.Tool.COVERITY.name, taskDetailVO.taskId)
                    compileRedLineReportServiceImpl.saveRedLineData(
                        taskDetailVO,
                        ComConstants.Tool.COVERITY.name,
                        buildId,
                        Lists.newArrayList()
                    )
                }
            }

            // 刷新维度的红线数据
            redLineReportServiceImpl.updateDimensionRedLineData(taskId, buildId)

            // 上报红线指标数据
            uploadRedLineIndicators(snapShotEntity, taskDetailVO, buildId)

            // 发送产出物报告
            uploadAnalyseSnapshot(snapShotEntity)
        }

        // 更新个人待处理信息
        refreshPersonalStatistic(taskDetailVO)

        // 通知发送
        val isGrayToolTask: Boolean = !taskDetailVO.projectId.isNullOrBlank()
                && taskDetailVO.projectId.startsWith(ComConstants.GRAY_PROJECT_PREFIX)
        if (isGrayToolTask) {
            logger.info(
                "gray tool task not send any notify, task id: {}, project id: {}",
                taskId, taskDetailVO.projectId
            )
        } else {
            // 若buildFlag不为空，则说明是新版插件，具备区分重试边界能力: non-redis
            // 但不保障：插件流程控制的"失败时自动重试"
            val needRedisLock = snapShotEntity.buildFlag == null
            val allowNotify = if (needRedisLock) {
                // 主要是防止插件扫描失败后用户进行重试，会突破isAllToolsComplete(..)的逻辑，造成资源无意义重复损耗
                RedisLock(
                    redisTemplate,
                    "NOTIFY_FOR_DEVOPS_cCALLBACK:TASK_ID:${taskId}:BUILD_ID:${buildId}",
                    TimeUnit.HOURS.toSeconds(12)
                ).tryLock()
            } else {
                true
            }

            if (allowNotify) {
                // 发送邮件
                val emailNotifyModel = EmailNotifyModel(taskId, buildId, EmailType.INSTANT)
                rabbitTemplate.convertAndSend(
                    EXCHANGE_CODECC_GENERAL_NOTIFY,
                    ROUTE_CODECC_EMAIL_NOTIFY,
                    emailNotifyModel,
                    getMQMessagePostProcessor(
                        taskDetailVO.notifyCustomInfo?.emailReceiverType,
                        taskId
                    )
                )

                // 发送企业微信
                val rtxRetStatus = resultStatus == ComConstants.RDMCoverityStatus.success.name
                val rtxNotifyModel = RtxNotifyModel(taskId, rtxRetStatus, buildId)
                rabbitTemplate.convertAndSend(
                    EXCHANGE_CODECC_GENERAL_NOTIFY,
                    ROUTE_CODECC_RTX_NOTIFY,
                    rtxNotifyModel,
                    getMQMessagePostProcessor(
                        taskDetailVO.notifyCustomInfo?.rtxReceiverType,
                        taskId
                    )
                )
            }
        }
        pipelineAfterCallBackOpsService.doAfterHandleDevopsCallBack(taskId, buildId)

    }


    /**
     * 延迟消息属性处理器
     * 注："遗留处理人"通知依赖个人待处理的统计数据，适当延迟发出通知
     */
    private fun getMQMessagePostProcessor(
        receiverType: String?,
        taskId: Long,
        delay: Int = 10 * 1000
    ): (Message) -> Message {
        if (ComConstants.EmailReceiverType.ONLY_AUTHOR.code() == receiverType) {
            logger.info("mq delay notify message, task id: {}", taskId)
            return { msg -> msg.apply { messageProperties.delay = delay } }
        } else {
            return { msg -> msg }
        }
    }

    /**
     * 更新个人待处理
     */
    private fun refreshPersonalStatistic(taskDetailVO: TaskDetailVO) {
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() == taskDetailVO.createFrom
            && ComConstants.EmailReceiverType.ONLY_AUTHOR.code() != taskDetailVO.notifyCustomInfo?.rtxReceiverType
            && ComConstants.EmailReceiverType.ONLY_AUTHOR.code() != taskDetailVO.notifyCustomInfo?.emailReceiverType
        ) {
            // 开源扫描，没有打开"遗留处理人"开关的一律不刷新个人待处理；减少DB压力
            logger.info(
                "task from gongfeng scan not refresh personal statistic, task id: {}",
                taskDetailVO.taskId
            )
        } else {
            val request = TaskPersonalStatisticRefreshReq(
                taskDetailVO.taskId,
                "from pipeline service #handleDevopsCallBack"
            )
            rabbitTemplate.convertAndSend(EXCHANGE_TASK_PERSONAL, ROUTE_TASK_PERSONAL, request)
        }
    }

    /**
     * 组装pcg回调接口
     */
    /*private fun assembleCallback(snapShotEntity: SnapShotEntity, taskDetailVO: TaskDetailVO) : CustomProjCallbackModel {
        val pcgRdCallBackModel = CustomProjCallbackModel()
        return with(pcgRdCallBackModel){
            taskId = snapShotEntity.taskId
            buildId = snapShotEntity.buildId
            url = taskDetailVO.customProjInfo.url
            if(null != snapShotEntity.toolSnapshotList && snapShotEntity.toolSnapshotList.isNotEmpty()){
                snapShotEntity.toolSnapshotList.forEach {
                    it.defectDetailUrl = null
                    it.defectReportUrl = null
                }
            }
            toolSnapshotList = snapShotEntity.toolSnapshotList
            this
        }
    }*/

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

        //停止流水线
        val shutdownResult = client.getDevopsService(ServiceBuildResource::class.java).manualShutdown(
            userName, projectId, pipelineId, buildId, channelCode)
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
     * 发送产出物报告
     */
    private fun uploadAnalyseSnapshot(snapShotEntity: SnapShotEntity) {
        try {
            val codeccCallback = CodeccCallback(
                projectId = snapShotEntity.projectId,
                pipelineId = snapShotEntity.pipelineId,
                taskId = snapShotEntity.taskId.toString(),
                buildId = snapShotEntity.buildId,
                toolSnapshotList = objectMapper.readValue(
                    objectMapper.writeValueAsString(snapShotEntity.toolSnapshotList),
                    object : TypeReference<List<Map<String, Any>>>() {})
            )
            DevopsProxy.projectIdThreadLocal.set(snapShotEntity.projectId)
            val callbackResult = client.getDevopsService(ExternalCodeccResource::class.java).callback(codeccCallback)
            logger.info("upload analyse snapshot result status is ok: ${callbackResult.isOk()}")
        } catch (t: Throwable) {
            logger.error("upload analyse snapshot error", t)
        }
    }

    /**
     * 上报红线质量数据
     */
    private fun uploadRedLineIndicators(
        snapShotEntity: SnapShotEntity,
        taskDetail: TaskDetailVO,
        buildId: String
    ) {
        // 上报红线指标数据
        val redLineIndicators = redLineReportServiceImpl.getPipelineCallback(taskDetail, buildId)
        val metadataCallback = MetadataCallback(
            elementType = redLineIndicators.elementType,
            taskId = taskDetail.pipelineTaskId ?: "",
            taskName = taskDetail.pipelineTaskName ?: "",
            data = redLineIndicators.data.map {
                MetadataCallback.CallbackHisMetadata(
                    enName = it.enName,
                    cnName = it.cnName,
                    detail = it.detail,
                    type = QualityDataType.valueOf(it.type.toUpperCase()),
                    msg = it.msg,
                    value = it.value,
                    extra = it.extra
                )
            }
        )

        DevopsProxy.projectIdThreadLocal.set(snapShotEntity.projectId)
        val callbackResult = client.getDevopsService(ExternalQualityResource::class.java).metadataCallback(
            snapShotEntity.projectId, snapShotEntity.pipelineId,
            snapShotEntity.buildId, metadataCallback
        )
        var status = false
        if (callbackResult.isOk()) {
            status = true
            logger.info("upload red line indicators success!")
        } else {
            logger.info("upload red line indicators failed!")
        }

        snapShotService.updateMetadataReportStatus(
            snapShotEntity.projectId,
            taskDetail.taskId,
            snapShotEntity.buildId,
            status,
            snapShotEntity.buildFlag
        )
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
        val finishStep = if (ComConstants.Tool.COVERITY.name == toolName || ComConstants.Tool.KLOCWORK.name == toolName){
            ComConstants.Step4Cov.DEFECT_SYNS.value()
        } else{
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
