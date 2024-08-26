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

import com.tencent.bk.codecc.defect.model.SnapShotEntity
import com.tencent.bk.codecc.defect.service.AfterDevopsCallBackOpsService
import com.tencent.bk.codecc.defect.vo.TaskPersonalStatisticRefreshReq
import com.tencent.bk.codecc.task.api.ServiceToolRestResource
import com.tencent.bk.codecc.task.enums.EmailType
import com.tencent.bk.codecc.task.pojo.EmailNotifyModel
import com.tencent.bk.codecc.task.pojo.RtxNotifyModel
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.redis.lock.RedisLock
import com.tencent.devops.common.web.mq.EXCHANGE_CODECC_GENERAL_NOTIFY
import com.tencent.devops.common.web.mq.EXCHANGE_TASK_PERSONAL
import com.tencent.devops.common.web.mq.ROUTE_CODECC_EMAIL_NOTIFY
import com.tencent.devops.common.web.mq.ROUTE_CODECC_RTX_NOTIFY
import com.tencent.devops.common.web.mq.ROUTE_TASK_PERSONAL
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class AfterDevopsCallBackOpsServiceImpl @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, String>,
    private val rabbitTemplate: RabbitTemplate,
    private val scanFinishEventService: ScanFinishEventService,
    private val client: Client
) : AfterDevopsCallBackOpsService {
    companion object {
        private val logger = LoggerFactory.getLogger(AfterDevopsCallBackOpsServiceImpl::class.java)
    }

    override fun doAfterHandleDevopsCallBack(
        taskId: Long,
        taskDetailVO: TaskDetailVO,
        buildId: String,
        snapShotEntity: SnapShotEntity,
        toolName: String
    ) {
        // 定义三个处理方法，完成后的处理也会有执行次序诉求
        preHandler(taskId, taskDetailVO, buildId, snapShotEntity, toolName)
        handler(taskId, taskDetailVO, buildId, snapShotEntity, toolName)
        postHandler(taskId, taskDetailVO, buildId, snapShotEntity, toolName)
    }

    protected fun preHandler(
        taskId: Long,
        taskDetailVO: TaskDetailVO,
        buildId: String,
        snapShotEntity: SnapShotEntity,
        toolName: String
    ) {
        // 检查是否已完成未运行的告警屏蔽
        checkAndWaitInvalidToolDefectFinish(taskId, buildId)
    }

    protected fun handler(
        taskId: Long,
        taskDetailVO: TaskDetailVO,
        buildId: String,
        snapShotEntity: SnapShotEntity,
        toolName: String
    ) {
        // 处理流水线任务工具排序
        processPipelineTaskToolOrder(taskDetailVO, buildId, snapShotEntity)
        // 更新个人待处理信息
        refreshPersonalStatistic(taskDetailVO)
        // 发送邮件
        sendEmailOrRtxNotify(taskId, taskDetailVO, buildId, snapShotEntity, toolName)
    }

    protected fun postHandler(
        taskId: Long,
        taskDetailVO: TaskDetailVO,
        buildId: String,
        snapShotEntity: SnapShotEntity,
        toolName: String
    ) {
        // 发送扫描结束事件
        scanFinishEventService.sendScanFinishEvent(taskId, buildId)
    }

    /**
     * 发送邮件或者企微通知
     */
    private fun sendEmailOrRtxNotify(
        taskId: Long,
        taskDetailVO: TaskDetailVO,
        buildId: String,
        snapShotEntity: SnapShotEntity,
        toolName: String
    ) {
        // 通知发送
        val isGrayToolTask: Boolean = !taskDetailVO.projectId.isNullOrBlank() &&
                taskDetailVO.projectId.startsWith(ComConstants.GRAY_PROJECT_PREFIX)
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
                    "NOTIFY_FOR_DEVOPS_cCALLBACK:TASK_ID:$taskId:BUILD_ID:$buildId",
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
                val resultStatus = snapShotEntity.toolSnapshotList.filter { it.toolNameEn == toolName }
                        .map { it.resultStatus }.firstOrNull()
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

    private fun checkAndWaitInvalidToolDefectFinish(taskId: Long, buildId: String) {
        // 检查是否所有的失效的工具都完成屏蔽,等待10s
        var waitCount = 0
        val redisKey = RedisKeyConstants.TASK_INVALID_TOOL_DEFECT + ":" + taskId + ":" + buildId
        while (true) {
            if (waitCount > 10) {
                break
            }
            val unfinishedToolCount = redisTemplate.opsForValue().get(redisKey)
            if (unfinishedToolCount == null || !StringUtils.isNumeric(unfinishedToolCount) ||
                    unfinishedToolCount.toLong() <= 0
            ) {
                break
            }
            waitCount++
            Thread.sleep(1000)
        }
    }

    /**
     * 更新个人待处理
     */
    private fun refreshPersonalStatistic(taskDetailVO: TaskDetailVO) {
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() == taskDetailVO.createFrom &&
                ComConstants.EmailReceiverType.ONLY_AUTHOR.code() != taskDetailVO.notifyCustomInfo?.rtxReceiverType &&
                ComConstants.EmailReceiverType.ONLY_AUTHOR.code() != taskDetailVO.notifyCustomInfo?.emailReceiverType
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
     * 流水线任务工具排序
     */
    private fun processPipelineTaskToolOrder(
        taskDetailVO: TaskDetailVO,
        buildId: String,
        snapShotEntity: SnapShotEntity
    ) {
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
        }
    }
}
