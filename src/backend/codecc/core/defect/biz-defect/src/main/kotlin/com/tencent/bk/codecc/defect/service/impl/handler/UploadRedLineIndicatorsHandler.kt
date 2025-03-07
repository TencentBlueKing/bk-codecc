package com.tencent.bk.codecc.defect.service.impl.handler

import com.google.common.collect.Lists
import com.tencent.bk.codecc.defect.pojo.HandlerDTO
import com.tencent.bk.codecc.defect.service.IHandler
import com.tencent.bk.codecc.defect.service.SnapShotService
import com.tencent.bk.codecc.defect.service.impl.redline.CompileRedLineReportServiceImpl
import com.tencent.bk.codecc.defect.service.impl.redline.RedLineReportServiceImpl
import com.tencent.bk.codecc.defect.utils.RedLineUtils
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.bk.codecc.task.constant.TaskMessageCode
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.api.ToolMetaBaseVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.codecc.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.proxy.DevopsProxy
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.service.ToolMetaCacheService
import com.tencent.devops.quality.api.v2.ExternalQualityResource
import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import com.tencent.devops.quality.api.v2.pojo.request.MetadataCallback
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UploadRedLineIndicatorsHandler @Autowired constructor(
    private val snapShotService: SnapShotService,
    private val client: Client,
    private val redLineReportServiceImpl: RedLineReportServiceImpl,
    private var compileRedLineReportServiceImpl: CompileRedLineReportServiceImpl,
    private var toolMetaCache: ToolMetaCacheService,
) : IHandler {
    companion object {
        private val logger = LoggerFactory.getLogger(UploadRedLineIndicatorsHandler::class.java)
    }

    override fun handler(handlerDTO: HandlerDTO) {
        try {
            if (handlerDTO.scanStatus == ComConstants.ScanStatus.SUCCESS) {
                // 查询任务详情
                val taskDetailVO = getTaskDetail(handlerDTO.taskId)
                // 流水线创建的，则处理产出报告以及红线
                if (RedLineUtils.checkIfTaskEnableRedLine(taskDetailVO)) {
                    // TODO 为了下架coverity工具后不会导致配制了coverity指标的质量红线被拦截，这里做个兼容，给coverity的指标都上报0
                    val hasCoverity = taskDetailVO.toolConfigInfoList.filter { toolConfigInfoVO ->
                        // TODO 临时修改测试，稍后回更
                        toolConfigInfoVO.followStatus != ComConstants.FOLLOW_STATUS.WITHDRAW.value()
                    }.any { toolConfigInfoVO ->
                        toolConfigInfoVO.toolName == ComConstants.Tool.COVERITY.name
                    }
                    if (hasCoverity) {
                        val toolMetaBase: ToolMetaBaseVO =
                            toolMetaCache.getToolBaseMetaCache(ComConstants.Tool.COVERITY.name)
                        if (ComConstants.ToolIntegratedStatus.D.name == toolMetaBase.status) {
                            logger.info(
                                "tool was removed: {}, {}",
                                ComConstants.Tool.COVERITY.name,
                                taskDetailVO.taskId
                            )
                            compileRedLineReportServiceImpl.saveRedLineData(
                                taskDetailVO,
                                ComConstants.Tool.COVERITY.name,
                                handlerDTO.buildId,
                                Lists.newArrayList()
                            )
                        }
                    }

                    // 刷新维度的红线数据
                    redLineReportServiceImpl.updateDimensionRedLineData(handlerDTO.taskId, handlerDTO.buildId)

                    // 上报质量红线
                    uploadRedLine(handlerDTO, taskDetailVO)
                }
            }
        } catch (e: Throwable) {
            logger.error(
                "upload readLine fail! ${handlerDTO.taskId}, ${handlerDTO.toolName}, ${handlerDTO.buildId}", e
            )
        }
    }

    private fun uploadRedLine(handlerDTO: HandlerDTO, taskDetailVO: TaskDetailVO) {
        logger.info("upload redLine: ${handlerDTO.taskId}-${handlerDTO.buildId}")
        // 上报红线指标数据
        val redLineIndicators = redLineReportServiceImpl.getPipelineCallback(taskDetailVO, handlerDTO.buildId)
        val metadataCallback = MetadataCallback(
            elementType = redLineIndicators.elementType,
            taskId = taskDetailVO.pipelineTaskId ?: "",
            taskName = taskDetailVO.pipelineTaskName ?: "",
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
        DevopsProxy.projectIdThreadLocal.set(taskDetailVO.projectId)
        val callbackResult = client.getDevopsService(ExternalQualityResource::class.java).metadataCallback(
            taskDetailVO.projectId, taskDetailVO.pipelineId,
            handlerDTO.buildId, metadataCallback
        )
        var status = false
        if (callbackResult.isOk()) {
            status = true
            logger.info("upload red line indicators success!")
        } else {
            logger.info("upload red line indicators failed!")
        }
        // 更新分析上报状态
        snapShotService.updateMetadataReportStatus(
            taskDetailVO.projectId, handlerDTO.buildId, taskDetailVO.taskId, status
        )
    }

    private fun getTaskDetail(taskId: Long): TaskDetailVO {
        val result: Result<TaskDetailVO?> = client.get(ServiceTaskRestResource::class.java)
            .getTaskInfoById(taskId)

        if (result.isNotOk() || result.data == null) {
            throw CodeCCException(TaskMessageCode.TASK_NOT_FOUND)
        }

        return result.data!!
    }
}
