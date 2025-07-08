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

package com.tencent.devops.common.web.aop

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_TASK_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.AUTHOR_TRANSFER
import com.tencent.devops.common.constant.ComConstants.BusinessType
import com.tencent.devops.common.constant.ComConstants.CLOSE_CHECKER
import com.tencent.devops.common.constant.ComConstants.CODE_COMMENT_ADD
import com.tencent.devops.common.constant.ComConstants.CODE_COMMENT_DEL
import com.tencent.devops.common.constant.ComConstants.CommonJudge
import com.tencent.devops.common.constant.ComConstants.DISABLE_ACTION
import com.tencent.devops.common.constant.ComConstants.ENABLE_ACTION
import com.tencent.devops.common.constant.ComConstants.FUNC_ASSIGN_DEFECT
import com.tencent.devops.common.constant.ComConstants.FUNC_BATCH_DEFECT
import com.tencent.devops.common.constant.ComConstants.FUNC_CHECKER_CONFIG
import com.tencent.devops.common.constant.ComConstants.FUNC_CODE_COMMENT_ADD
import com.tencent.devops.common.constant.ComConstants.FUNC_CODE_COMMENT_DEL
import com.tencent.devops.common.constant.ComConstants.FUNC_CODE_REPOSITORY
import com.tencent.devops.common.constant.ComConstants.FUNC_DEFECT_IGNORE
import com.tencent.devops.common.constant.ComConstants.FUNC_DEFECT_MANAGE
import com.tencent.devops.common.constant.ComConstants.FUNC_DEFECT_MARKED
import com.tencent.devops.common.constant.ComConstants.FUNC_DEFECT_UNMARKED
import com.tencent.devops.common.constant.ComConstants.FUNC_FILTER_PATH
import com.tencent.devops.common.constant.ComConstants.FUNC_ISSUE_DEFECT
import com.tencent.devops.common.constant.ComConstants.FUNC_REGISTER_TOOL
import com.tencent.devops.common.constant.ComConstants.FUNC_REVERT_IGNORE
import com.tencent.devops.common.constant.ComConstants.FUNC_SCAN_SCHEDULE
import com.tencent.devops.common.constant.ComConstants.FUNC_TASK_INFO
import com.tencent.devops.common.constant.ComConstants.FUNC_TASK_SWITCH
import com.tencent.devops.common.constant.ComConstants.FUNC_TOOL_SWITCH
import com.tencent.devops.common.constant.ComConstants.FUNC_TRIGGER_ANALYSIS
import com.tencent.devops.common.constant.ComConstants.OPEN_CHECKER
import com.tencent.devops.common.web.aop.annotation.OperationHistory
import com.tencent.devops.common.web.aop.model.OperationHistoryDTO
import com.tencent.devops.common.web.mq.EXCHANGE_OPERATION_HISTORY
import com.tencent.devops.common.web.mq.ROUTE_OPERATION_HISTORY
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.apache.commons.lang.StringUtils
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * 上报分析记录aop方法
 *
 * @date 2019/6/14
 * @version V1.0
 */
@Aspect
class OperationHistoryAop @Autowired constructor(
    val rabbitTemplate: RabbitTemplate
) {

    companion object {
        private val logger = LoggerFactory.getLogger(OperationHistoryAop::class.java)
    }

    @Pointcut("@annotation(com.tencent.devops.common.web.aop.annotation.OperationHistory)")
    fun operationHistory() {
    }

    @AfterReturning("operationHistory()&&@annotation(annotation)")
    fun reportOperationHistory(
        joinPoint: JoinPoint,
        annotation: OperationHistory
    ) {
        //获取功能id
        val funcId = getFuncId(joinPoint, annotation.funcId)
        if (funcId.isBlank()) {
            logger.error("operation history aborted!!")
            return
        }
        //获取操作类型
        val operType = getOperType(joinPoint, funcId, annotation.operType)
        //获取特定工具
        var toolName: String? = null
        if (funcId == FUNC_DEFECT_MANAGE || funcId == FUNC_CHECKER_CONFIG || funcId == FUNC_CODE_COMMENT_ADD
                || funcId == FUNC_CODE_COMMENT_DEL
        ) {
            val objects = joinPoint.args
            when (operType) {
                AUTHOR_TRANSFER -> {
                    val jsonObject = JSONObject.fromObject(objects[0])
                }
                OPEN_CHECKER, CLOSE_CHECKER -> {
                    val jsonObject = JSONObject.fromObject(objects[2])
                    toolName = jsonObject.getString("toolName")
                }
                CODE_COMMENT_ADD -> {
                    toolName = objects[1].toString()
                }
                CODE_COMMENT_DEL -> {
                    toolName = objects[2].toString()
                }
            }
        }
        // 批处理告警 支持多种工具pattern 需要传工具维度
        var dimension: String? = null
        var operaMsg: String? = null
        var operaTypeName: String? = null
        if (annotation.funcId == FUNC_BATCH_DEFECT) {
            val objects = joinPoint.args
            val jsonObject = JSONObject.fromObject(objects[0])
            dimension = jsonObject.getJSONArray("dimensionList").toArray().joinToString(",")
            if (toolName.isNullOrBlank()) {
                toolName = jsonObject.getJSONArray("toolNameList").toArray().joinToString(",")
            }

            // 若是全选/按文件维度操作,需要保留查询条件
            if (jsonObject.getString("isSelectAll") == CommonJudge.COMMON_Y.value()) {
                operaMsg = jsonObject.getString("queryDefectCondition")
                operaTypeName = CommonJudge.COMMON_Y.value()
            }
        }

        //获取任务id
        val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
        val taskId = if (StringUtils.isBlank(request.getHeader(AUTH_HEADER_DEVOPS_TASK_ID))) {
            0L
        } else {
            request.getHeader(AUTH_HEADER_DEVOPS_TASK_ID).toLong()
        }
        // 获取流水线id
        val pipelineId = request.getParameter("pipelineId") ?: ""
        // 获取单流水线对应多任务标识
        val multiPipelineMark = request.getParameter("multiPipelineMark")
        //获取操作用户
        val userName = request.getHeader(AUTH_HEADER_DEVOPS_USER_ID) ?: request.getParameter("userName")
        //获取操作消息
        val paramArray = getParamArray(joinPoint, funcId, userName)
        //获取当前时间
        val currentTime = System.currentTimeMillis()
        // 获取项目的id
        val projectId = request.getHeader(AUTH_HEADER_DEVOPS_PROJECT_ID) ?: request.getParameter("projectId")
        val operationHistoryDTO = OperationHistoryDTO(
            taskId = taskId,
            pipelineId = pipelineId,
            multiPipelineMark = multiPipelineMark,
            funcId = funcId,
            operType = operType,
            operTypeName = operaTypeName,
            time = currentTime,
            paramArray = paramArray,
            operMsg = operaMsg,
            toolName = toolName,
            dimension = dimension,
            operator = userName,
            projectId = projectId
        )
        logger.info(">>>>>>>>>>>>>>> operationHistoryDTO: $operationHistoryDTO")
        //发送消息，异步处理
        rabbitTemplate.convertAndSend(
            EXCHANGE_OPERATION_HISTORY,
            ROUTE_OPERATION_HISTORY, operationHistoryDTO
        )
    }


    /**
     * 获取操作类型
     */
    private fun getOperType(
        joinPoint: JoinPoint, funcId: String,
        operType: String
    ): String {
        val objects = joinPoint.args
        return with(funcId)
        {
            when (this) {
                //停用启用任务
                FUNC_TOOL_SWITCH -> {
                    val manageType = objects[1] as String
                    if (ComConstants.CommonJudge.COMMON_Y.value() == manageType) {
                        ENABLE_ACTION
                    } else {
                        DISABLE_ACTION
                    }
                }
                // 打开关闭规则配置
                FUNC_CHECKER_CONFIG -> {
                    val checker = JSONObject.fromObject(objects[2])
                    if ((checker.get("openedCheckers") as JSONArray).size > 0) {
                        OPEN_CHECKER
                    } else {
                        CLOSE_CHECKER
                    }
                }
                FUNC_DEFECT_IGNORE, FUNC_REVERT_IGNORE, FUNC_DEFECT_MARKED, FUNC_DEFECT_UNMARKED,
                FUNC_ASSIGN_DEFECT, FUNC_ISSUE_DEFECT -> {
                    funcId
                }
                else -> {
                    operType
                }
            }
        }
    }


    /**
     * 获取操作记录消息
     */
    private fun getParamArray(joinPoint: JoinPoint, funcId: String, user: String): Array<String> {

        val objects = joinPoint.args
        return with(funcId)
        {
            when (this) {
                //注册工具功能
                FUNC_REGISTER_TOOL -> {
                    val jsonObject = JSONObject.fromObject(objects[0])
                    val toolArray = jsonObject.getJSONArray("tools")
                    val toolName = toolArray.map { tool ->
                        (tool as JSONObject).getString("toolName")
                    }.reduce { acc, s -> "$acc,$s" }
                    arrayOf(user, toolName)
                }
                //修改任务信息功能
                FUNC_TASK_INFO -> {
                    arrayOf(user)
                }
                //停用启用任务
                FUNC_TASK_SWITCH -> {
                    arrayOf(user)
                }
                //停用启用工具
                FUNC_TOOL_SWITCH -> {
                    arrayOf(user)
                }
                //触发立即分析
                FUNC_TRIGGER_ANALYSIS -> {
                    arrayOf(user)
                }
                //定时扫描分析
                FUNC_SCAN_SCHEDULE -> {
                    arrayOf(user)
                }
                //操作屏蔽路径
                FUNC_FILTER_PATH -> {
                    arrayOf(user)
                }
                //作者批量转换
                FUNC_DEFECT_MANAGE -> {
                    val jsonObject = JSONObject.fromObject(objects[1])
                    val toolName = jsonObject.getString("toolName")
                    val sourceAuthor = jsonObject.getJSONArray("sourceAuthor")
                    val targetAuthor = jsonObject.getJSONArray("targetAuthor")
                    arrayOf(user, toolName, sourceAuthor.join(","), targetAuthor.join(","))
                }
                //任务代码库更新
                FUNC_CODE_REPOSITORY -> {
                    arrayOf(user)
                }
                FUNC_DEFECT_IGNORE -> {
                    val jsonObject = JSONObject.fromObject(objects[0])
                    // array 判空 转set
                    val defectEntityIdSet = jsonArray2Set(jsonArray = jsonObject.getJSONArray("defectKeySet"))

                    val ignoreReasonType = jsonObject.getString("ignoreReasonType")

                    val ignoreReason = jsonObject.getString("ignoreReason")

                    arrayOf(defectEntityIdSet.joinToString(","), ignoreReasonType, ignoreReason)
                }
                FUNC_REVERT_IGNORE, FUNC_DEFECT_MARKED, FUNC_DEFECT_UNMARKED, FUNC_ISSUE_DEFECT -> {
                    val jsonObject = JSONObject.fromObject(objects[0])
                    val defectKeySet = jsonArray2Set(jsonArray = jsonObject.getJSONArray("defectKeySet"))
                    arrayOf(defectKeySet.joinToString(","))
                }
                FUNC_ASSIGN_DEFECT -> {
                    val jsonObject = JSONObject.fromObject(objects[0])
                    val defectKeySet = jsonArray2Set(jsonArray = jsonObject.getJSONArray("defectKeySet"))
                    val sourceAuthorSet = jsonArray2Set(jsonArray = jsonObject.getJSONArray("sourceAuthor"))
                    val newAuthorSet = jsonArray2Set(jsonArray = jsonObject.getJSONArray("newAuthor"))
                    arrayOf(
                        defectKeySet.joinToString(","), sourceAuthorSet.joinToString(","),
                        newAuthorSet.joinToString(",")
                    )
                }
                FUNC_CODE_COMMENT_ADD -> {
                    val defectId = objects[0].toString()
                    val jsonObject = JSONObject.fromObject(objects[4])
                    val comment = jsonObject.getString("comment")
                    arrayOf(defectId, comment)
                }
                FUNC_CODE_COMMENT_DEL -> {
                    val defectId = objects[4].toString()
                    val comment = objects[5].toString()
                    arrayOf(defectId, comment)
                }
                else -> {
                    arrayOf(user)
                }
            }
        }
    }


    /**
     * 批处理告警的FUNC ID 需要细分处理
     */
    private fun getFuncId(joinPoint: JoinPoint, funcId: String): String {
        val objects = joinPoint.args
        return with(funcId) {
            when (this) {
                FUNC_BATCH_DEFECT -> {
                    val jsonObject = JSONObject.fromObject(objects[0])
                    val bizType = jsonObject.getString("bizType")
                    if (BusinessType.IGNORE_DEFECT.value() == bizType) {
                        FUNC_DEFECT_IGNORE
                    } else if (BusinessType.REVERT_IGNORE.value() == bizType) {
                        FUNC_REVERT_IGNORE
                    } else if (BusinessType.MARK_DEFECT.value() == bizType) {
                        // 标志修改，0表示取消标志，1表示标志修改
                        if (jsonObject.getInt("markFlag") == 1) {
                            FUNC_DEFECT_MARKED
                        } else {
                            FUNC_DEFECT_UNMARKED
                        }
                        // 告警提单操作
                    } else if ("IssueDefect" == bizType) {
                        FUNC_ISSUE_DEFECT
                    } else if (BusinessType.ASSIGN_DEFECT.value() == bizType) {
                        FUNC_ASSIGN_DEFECT
                    } else {
                        // 未识别的批处理操作
                        logger.warn("Unidentified batch operation! bizType: $bizType")
                        ""
                    }
                }
                else -> {
                    funcId
                }
            }
        }
    }

    /**
     * JSONArray转Set
     */
    private fun jsonArray2Set(jsonArray: JSONArray): Set<String> {
        val defectEntityIdSet = mutableSetOf<String>()
        if (jsonArray.isNotEmpty()) {
            (0 until jsonArray.size).mapTo(defectEntityIdSet) { jsonArray[it].toString() }
        }
        return defectEntityIdSet
    }
}