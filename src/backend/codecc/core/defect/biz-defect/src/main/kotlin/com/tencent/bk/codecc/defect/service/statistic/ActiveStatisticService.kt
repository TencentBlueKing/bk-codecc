/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.service.statistic

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.ScanCodeSummaryRepository
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.TaskLogRepository
import com.tencent.bk.codecc.defect.model.ScanCodeSummaryEntity
import com.tencent.bk.codecc.defect.model.statistic.CLOCStatisticEntity
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO
import com.tencent.devops.common.auth.api.pojo.external.KEY_CREATE_FROM
import com.tencent.devops.common.auth.api.pojo.external.KEY_PROJECT_ID
import com.tencent.devops.common.auth.api.pojo.external.PREFIX_TASK_INFO
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.DefectStatType.getDataFromByProjectId
import com.tencent.devops.common.constant.ComConstants.ScanStatType
import com.tencent.devops.common.constant.ComConstants.TOTAL_BLANK
import com.tencent.devops.common.constant.ComConstants.TOTAL_CODE
import com.tencent.devops.common.constant.ComConstants.TOTAL_COMMENT
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.redis.lock.RedisLock
import com.tencent.devops.common.util.DateTimeUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class ActiveStatisticService @Autowired constructor(
    private val taskLogRepository: TaskLogRepository,
    private val scanCodeSummaryRepository: ScanCodeSummaryRepository,
    private val thirdPartySystemCaller: ThirdPartySystemCaller,
    private val redisTemplate: RedisTemplate<String, String>
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ActiveStatisticService::class.java)
        private val COMMON_TOOLS = mutableListOf(ComConstants.Tool.COVERITY.name, ComConstants.Tool.KLOCWORK.name,
                ComConstants.Tool.PINPOINT.name)
        private const val REDIS_LOCK_KEY_PREFIX = "LOCK_KEY"
        private const val REDIS_KEY_EXPIRE_TIME: Long = 8
    }

    /**
     * 统计任务工具分析记录
     */
    fun statTaskAndTool(uploadTaskLogStepVO: UploadTaskLogStepVO) {
        logger.info("active statistic before analyze -> taskLogStepVO: {}", uploadTaskLogStepVO)

        val voTaskId = uploadTaskLogStepVO.taskId
        val voToolName = uploadTaskLogStepVO.toolName
        val voStepNum = uploadTaskLogStepVO.stepNum
        val voFlag = uploadTaskLogStepVO.flag
        val voBuildId = uploadTaskLogStepVO.pipelineBuildId

        val isLastStepNum = if (COMMON_TOOLS.contains(
                voToolName)) ComConstants.Step4Cov.DEFECT_SYNS.value() else ComConstants.Step4MutliTool.COMMIT.value()

        val dateStr = DateTimeUtils.getDateByDiff(0)

        // 修复任务ID可能为空
        val finalTaskId = getFinalTaskIdStr(voTaskId, uploadTaskLogStepVO.streamName)

        val taskCreateFrom =
            redisTemplate.opsForHash<String, String>().get(PREFIX_TASK_INFO + finalTaskId, KEY_CREATE_FROM)
        val projectId =
            redisTemplate.opsForHash<String, String>().get(PREFIX_TASK_INFO + finalTaskId, KEY_PROJECT_ID)
        val dataFrom = getDataFromByProjectId(taskCreateFrom!!, projectId!!)

        // 统计活跃
        if (voStepNum == 1) {
            // 活跃任务
            val taskKey = "${RedisKeyConstants.PREFIX_ACTIVE_TASK}$dateStr:$dataFrom"
            redisTemplate.opsForSet().add(taskKey, finalTaskId)
            logger.info("active statistic save.")
        }

        // 区分超快和非超快
        val scanStatTypeStr = if (uploadTaskLogStepVO.isFastIncrement) {
            ScanStatType.IS_FAST_INCREMENT.value
        } else {
            ScanStatType.NOT_FAST_INCREMENT.value
        }

        // 统计分析失败次数 2、4
        if (voFlag == ComConstants.StepFlag.FAIL.value() || voFlag == ComConstants.StepFlag.ABORT.value()) {
            // 判断是否已统计过该buildId
            val toolBuildIdKey = "${RedisKeyConstants.PREDIX_TOOL_BUILD_ID_SET}$dateStr:$dataFrom:$voToolName"
            if (redisTemplate.opsForSet().isMember(toolBuildIdKey, voBuildId) != true) {
                // 记录已统计标记
                redisTemplate.opsForSet().add(toolBuildIdKey, voBuildId)
                this.setExpiredKeyByDay(toolBuildIdKey)

                val analyzeFailKey =
                    "${RedisKeyConstants.PREFIX_ANALYZE_FAIL_COUNT}$dateStr:$dataFrom:$voToolName"
                redisTemplate.opsForList().rightPush(analyzeFailKey, finalTaskId)

                // 记录每天各工具分析失败次数(按是否超快)
                val analyzeFailToolKey =
                    "${RedisKeyConstants.PREFIX_ANALYZE_FAIL_TOOL}$dateStr:$dataFrom:$scanStatTypeStr"
                redisTemplate.opsForHash<String, String>().increment(analyzeFailToolKey, voToolName, 1)
                this.setExpiredKeyByDay(analyzeFailToolKey)
                logger.info("analyze fail statistic save.")
            }
        }

        // 统计分析成功次数 1
        if (voStepNum == isLastStepNum && voFlag == ComConstants.StepFlag.SUCC.value()) {
            // 判断是否已统计过该buildId
            val toolBuildIdKey = "${RedisKeyConstants.PREDIX_TOOL_BUILD_ID_SET}$dateStr:$dataFrom:$voToolName"
            if (redisTemplate.opsForSet().isMember(toolBuildIdKey, voBuildId) != true) {
                // 记录已统计标记
                redisTemplate.opsForSet().add(toolBuildIdKey, voBuildId)
                this.setExpiredKeyByDay(toolBuildIdKey)

                val analyzeSuccessKey = "${RedisKeyConstants.PREFIX_ANALYZE_SUCC_COUNT}$dateStr:$dataFrom:$voToolName"
                redisTemplate.opsForList().rightPush(analyzeSuccessKey, finalTaskId)
                this.setExpiredKeyByDay(analyzeSuccessKey)
                logger.info("analyze success statistic save.")

                // 记录工具分析耗时
                val lastTaskLogEntity =
                        taskLogRepository.findFirstByTaskIdAndToolNameAndBuildId(voTaskId, voToolName, voBuildId)
                val elapseTime = uploadTaskLogStepVO.endTime - lastTaskLogEntity.startTime

                // 记录每天累计耗时
                val elapseTimeKey =
                        "${RedisKeyConstants.PREFIX_ANALYZE_SUCC_ELAPSE_TIME}$dateStr:$dataFrom:$scanStatTypeStr"
                val elapseTimeStr = redisTemplate.opsForHash<String, String>().get(elapseTimeKey, voToolName)
                val elapseTimeLong = (elapseTimeStr?.toLongOrNull() ?: 0L) + elapseTime
                redisTemplate.opsForHash<String, String>().put(elapseTimeKey, voToolName, elapseTimeLong.toString())
                this.setExpiredKeyByDay(elapseTimeKey)

                // 记录每天各工具分析成功次数(按是否超快)
                val analyzeSuccToolKey =
                        "${RedisKeyConstants.PREFIX_ANALYZE_SUCC_TOOL}$dateStr:$dataFrom:$scanStatTypeStr"
                redisTemplate.opsForHash<String, String>().increment(analyzeSuccToolKey, voToolName, 1)
                this.setExpiredKeyByDay(analyzeSuccToolKey)

                logger.info("tool analyze elapse time statistic save.")
            }
        }

        logger.info("statistic finish.")
    }

    private fun getFinalTaskIdStr(voTaskId: Long, nameEn: String): String {
        val finalTaskId = if (voTaskId == 0L) {
            thirdPartySystemCaller.getTaskInfoWithoutToolsByStreamName(nameEn)
                .let { taskDetailVO ->
                    redisTemplate.opsForHash<String, String>()
                        .put(PREFIX_TASK_INFO + taskDetailVO.taskId, KEY_PROJECT_ID, taskDetailVO.projectId)
                    taskDetailVO.taskId.toString()
                }
        } else {
            redisTemplate.opsForHash<String, String>().get(PREFIX_TASK_INFO + voTaskId, KEY_PROJECT_ID) ?: run {
                thirdPartySystemCaller.getTaskInfoWithoutToolsByTaskId(voTaskId).let { taskDetailVO ->
                    redisTemplate.opsForHash<String, String>()
                        .put(PREFIX_TASK_INFO + taskDetailVO.taskId, KEY_PROJECT_ID, taskDetailVO.projectId)
                }
            }
            voTaskId.toString()
        }
        return finalTaskId
    }

    /**
     * 统计代码行
     */
    fun statCodeLineByCloc(clocStatisticEntity: Collection<CLOCStatisticEntity>, scanStatType: ScanStatType) {
        logger.info("active statistic after upload -> size: {}", clocStatisticEntity.size)
        val taskId = clocStatisticEntity.first().taskId
        val finalTaskInfoKey = PREFIX_TASK_INFO + taskId
        // 根据任务创建来源taskCreateFrom和项目id判定
        val taskCreateFrom = redisTemplate.opsForHash<String, String>().get(finalTaskInfoKey, KEY_CREATE_FROM)
            ?: thirdPartySystemCaller.getTaskInfoWithoutToolsByTaskId(taskId).createFrom.also {
                redisTemplate.opsForHash<String, String>().put(finalTaskInfoKey, KEY_CREATE_FROM, it)
            }
        val projectId = redisTemplate.opsForHash<String, String>().get(finalTaskInfoKey, KEY_PROJECT_ID)
            ?: thirdPartySystemCaller.getTaskInfoWithoutToolsByTaskId(taskId).projectId.also {
                redisTemplate.opsForHash<String, String>().put(finalTaskInfoKey, KEY_PROJECT_ID, it)
            }

        val createFrom = getDataFromByProjectId(taskCreateFrom, projectId)

        val currentDate = DateTimeUtils.getDateByDiff(0)
        val key = "${RedisKeyConstants.CODE_LINE_STAT}$currentDate:$createFrom"
        val currentMap = redisTemplate.opsForHash<String, String>().entries(key).toMutableMap().apply {
            putIfAbsent(TOTAL_BLANK, "0")
            putIfAbsent(TOTAL_COMMENT, "0")
            putIfAbsent(TOTAL_CODE, "0")
        }

        val (totalBlank, totalComment, totalCode) = clocStatisticEntity.fold(Triple(0L, 0L, 0L)) { acc, entity ->
            Triple(
                acc.first + entity.sumBlank,
                acc.second + entity.sumComment,
                acc.third + entity.sumCode
            )
        }

        currentMap[TOTAL_BLANK] = (totalBlank + (currentMap[TOTAL_BLANK]!!.toLong())).toString()
        currentMap[TOTAL_COMMENT] = (totalComment + (currentMap[TOTAL_COMMENT]!!.toLong())).toString()
        currentMap[TOTAL_CODE] = (totalCode + (currentMap[TOTAL_CODE]!!.toLong())).toString()
        redisTemplate.opsForHash<String, String>().putAll(key, currentMap)
        this.setExpiredKeyByDay(key)

        logger.info("statistic code line finish.")

        scanCodeSummaryStatAndSave(
            taskId = taskId,
            scanStatType = scanStatType,
            clocStatisticEntity = clocStatisticEntity
        )
    }

    /**
     * 扫描代码行汇总统计并保存
     */
    private fun scanCodeSummaryStatAndSave(
        taskId: Long,
        scanStatType: ScanStatType,
        clocStatisticEntity: Collection<CLOCStatisticEntity>
    ) {
        val buildId = clocStatisticEntity.first().buildId
        val taskDetailVO = thirdPartySystemCaller.getTaskInfoWithoutToolsByTaskId(taskId)

        val lock = RedisLock(redisTemplate, "$REDIS_LOCK_KEY_PREFIX:$taskId:$buildId", TimeUnit.SECONDS.toSeconds(3))
        try {
            // 集群只消费1次，锁期间的后来者当重复直接丢弃
            if (!lock.tryLock()) {
                logger.info("scanCodeSummaryStatAndSave, get lock fail, drop this record: $taskId, $buildId")
                return
            }

            val totalBlank = clocStatisticEntity.sumOf { if (it.sumBlank > 0) it.sumBlank else 0 }
            val totalComment = clocStatisticEntity.sumOf { if (it.sumComment > 0) it.sumComment else 0 }
            val totalCode = clocStatisticEntity.sumOf { if (it.sumCode > 0) it.sumCode else 0 }
            val entity = ScanCodeSummaryEntity().apply {
                this.taskId = taskId
                this.buildId = buildId
                this.scanType = scanStatType.value
                this.totalBlank = totalBlank
                this.totalComment = totalComment
                this.totalCode = totalCode
                this.totalLine = totalBlank + totalComment + totalCode
                this.scanFinishTime = clocStatisticEntity.first().updatedDate
                this.projectId = taskDetailVO.projectId
                this.bgId = taskDetailVO.bgId
                this.createFrom = taskDetailVO.createFrom
            }.also { it.applyAuditInfoOnCreate() }

            scanCodeSummaryRepository.save(entity)
        } catch (t: Throwable) {
            logger.error("summary code line error: $taskId, $buildId", t)
        } finally {
            if (lock.isLocked()) {
                lock.unlock()
            }
        }
    }

    /**
     * 设置过期时间(天)
     */
    private fun setExpiredKeyByDay(key: String, timeOut: Long = REDIS_KEY_EXPIRE_TIME) {
        val ttl = redisTemplate.getExpire(key, TimeUnit.DAYS)
        if (ttl == -1L) {
            redisTemplate.expire(key, timeOut, TimeUnit.DAYS)
        }
    }
}
