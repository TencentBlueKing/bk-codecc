package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.pojo.HandlerDTO
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CalTaskStatusServiceImpl @Autowired constructor(
    private val client: Client,
    private val taskLogService: TaskLogServiceImpl,
    private val taskLogOverviewService: TaskLogOverviewService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CalTaskStatusServiceImpl::class.java)
    }

    /**
     * 获取任务执行状态
     * 当所有工具都执行成功时才标记成功
     *
     * @param taskId
     * @param buildId
     */
    fun getTaskStatus(handlerDTO: HandlerDTO) {
        with(handlerDTO) {
            val taskLogVOList = taskLogService.getCurrBuildInfo(taskId, buildId)
            // 获取任务扫描工具
            val result = client.get(ServiceTaskRestResource::class.java).getTaskToolList(taskId)
            if (result.isNotOk() || result.data == null) {
                // 远程调用失败标记为为执行，不再计算度量信息
                    scanStatus = ComConstants.ScanStatus.FAIL
                logger.error("get task tool config info from remote fail! message:" +
                    " ${result.message} taskId: $taskId | buildId: $buildId")
            }

            val toolList = result.data
            // 获取任务的实际执行工具
            val actualExeTools = taskLogOverviewService.getActualExeTools(taskId, buildId)
            // 判断任务是否执行完毕的时候根据任务设置的扫描工具和实际扫描的工具决定
            toolList?.enableToolList?.filter { toolConfigBaseVO ->
                actualExeTools?.contains(toolConfigBaseVO.toolName) ?: true
            }?.forEach { tool ->
                    val taskLog = taskLogVOList.find { taskLogVO ->
                        taskLogVO.toolName.equals(tool.toolName, true)
                    }

                    if (taskLog == null) {
                        scanStatus = ComConstants.ScanStatus.FAIL
                        logger.error("${tool.toolName} " +
                            "not found! taskId: $taskId | buildId: $buildId")
                        return@with
                    }

                    // 执行成功则继续分析
                    if (taskLog.flag != ComConstants.StepFlag.SUCC.value()) {
                        scanStatus = if (taskLog.flag == ComConstants.StepFlag.PROCESSING.value()) {
                            logger.info("${taskLog.toolName} " +
                                "executing! taskId: $taskId | buildId: $buildId")
                            ComConstants.ScanStatus.PROCESSING
                        } else {
                            logger.error("${taskLog.toolName} " +
                                "execute not success! taskId: $taskId | buildId: $buildId")
                            ComConstants.ScanStatus.FAIL
                        }
                        return@with
                    }
                }
            logger.info("task execute success: $taskId $buildId")
            scanStatus = ComConstants.ScanStatus.SUCCESS
        }
    }
}
