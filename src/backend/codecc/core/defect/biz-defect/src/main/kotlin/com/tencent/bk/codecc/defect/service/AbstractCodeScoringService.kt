package com.tencent.bk.codecc.defect.service

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CLOCStatisticRepository
import com.tencent.bk.codecc.defect.dao.core.mongorepository.RedLineMetaRepository
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.RedLineRepository
import com.tencent.bk.codecc.defect.model.MetricsEntity
import com.tencent.bk.codecc.defect.model.pipelinereport.RedLineEntity
import com.tencent.bk.codecc.defect.pojo.StandardScoringConfig
import com.tencent.bk.codecc.defect.utils.CommonKafkaClient
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.codecc.util.JsonUtil
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.redis.lock.RedisLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
abstract class AbstractCodeScoringService @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, String>,
    private val commonKafkaClient: CommonKafkaClient,
    private val clocStatisticRepository: CLOCStatisticRepository,
    private val redLineRepository: RedLineRepository,
    private val redLineMetaRepository: RedLineMetaRepository
) {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    abstract fun scoring(taskDetailVO: TaskDetailVO, buildId: String): MetricsEntity?

    /**
     * 通用代码度量计算逻辑，普通扫描和超快增量都走这里
     * 当本次工具上报完成后其他工具上报未完成时不执行度量计算
     * 必须等所有工具执行完成并成功才执行度量计算逻辑
     *
     * @param taskDetailVO 任务信息实体
     * @param buildId 构建号
     * @param toolName 工具名称
     */
    fun scoring(taskDetailVO: TaskDetailVO, buildId: String, toolName: String) {

        val redisLock = RedisLock(
            redisTemplate = redisTemplate,
            lockKey = "${RedisKeyConstants.TASK_CODE_SCORING}${taskDetailVO.taskId}:$buildId",
            expiredTimeInSeconds = 5
        )
        try {
            if (redisLock.tryLock()) {
                logger.info(
                    "get redis lock: taskId: {} | buildId: {} | toolName: {}",
                    taskDetailVO.taskId,
                    buildId,
                    toolName
                )
                val metricsEntity = scoring(taskDetailVO, buildId) ?: return
                // 上报数据到数据平台
                commonKafkaClient.pushMetricsToKafka(metricsEntity)
            }
        } catch (e: Exception) {
            throw e
        } finally {
            redisLock.unlock()
        }
    }

    fun saveScoreRedLine(metricsEntity: MetricsEntity) {
        val currentTimeMillis = System.currentTimeMillis()
        val scoreRedLineMetas = redLineMetaRepository.findByDetail("SCORE")
        val scoreRedLineEntities = mutableListOf<RedLineEntity>()
        for (scoreRedLineMeta in scoreRedLineMetas) {
            val redLineEntity = RedLineEntity()
            val scoreRedLineName = scoreRedLineMeta.enName
            redLineEntity.buildId = metricsEntity.buildId
            redLineEntity.taskId = metricsEntity.taskId
            redLineEntity.type = scoreRedLineMeta.type
            redLineEntity.enName = scoreRedLineMeta.enName
            redLineEntity.cnName = scoreRedLineMeta.cnName
            redLineEntity.detail = scoreRedLineMeta.detail
            redLineEntity.msg = ""
            redLineEntity.extra = ""
            redLineEntity.createdDate = currentTimeMillis
            redLineEntity.updatedDate = currentTimeMillis
            redLineEntity.applyAuditInfoOnCreate()
            if (scoreRedLineName.contains(ComConstants.ScoreRedLineEnum.CODE_STANDARD_SCORE.name)) {
                // 代码规范得分
                redLineEntity.value = metricsEntity.codeStyleScore.toString()
            } else if (scoreRedLineName.contains(ComConstants.ScoreRedLineEnum.CODE_SECURITY_SCORE.name)) {
                // 安全漏洞
                redLineEntity.value = metricsEntity.codeSecurityScore.toString()
            } else if (scoreRedLineName.contains(ComConstants.ScoreRedLineEnum.CODE_CCN_SCORE.name)) {
                // 圈复杂度
                redLineEntity.value = metricsEntity.codeCcnScore.toString()
            }
            scoreRedLineEntities.add(redLineEntity)
        }
        redLineRepository.saveAll(scoreRedLineEntities)
    }

    /**
     * 取当前任务设置的语言集合,
     * 根据 codeLang 与各语言数值 按位与 大于0的为当前任务绑定的语言
     * @param taskDetailVO
     */
    fun initScoringConfigs(
        taskDetailVO: TaskDetailVO,
        lines: MutableMap<String, Long>
    ): MutableMap<ComConstants.CodeLang, StandardScoringConfig> {
        // 用 lines 的拷贝对象计算，防止 lines 元素被删减导致后续计算错误
        val copyLines = mutableMapOf<String, Long>()
        copyLines.putAll(lines)
        val languageList = mutableListOf<ComConstants.CodeLang>()
        ComConstants.CodeLang.values().filter {
            it != ComConstants.CodeLang.OTHERS
        }.forEach {
            if (taskDetailVO.codeLang.and(it.langValue()) > 0) {
                languageList.add(it)
            }
        }

        if (languageList.isEmpty()) {
            logger.warn("initScoringConfigs languageList is empty, task id: ${taskDetailVO.taskId}")
        }

        val scoringConfigs = mutableMapOf<ComConstants.CodeLang, StandardScoringConfig>()
        val iterator = languageList.iterator()
        while (iterator.hasNext()) {
            val lang = iterator.next()
            val configJsonStr: String? = redisTemplate.opsForHash<String, String>()
                .get(RedisKeyConstants.STANDARD_LANG, lang.langName())
            if (!configJsonStr.isNullOrBlank()) {
                val standardScoringConfig = JsonUtil.to(
                    configJsonStr,
                    StandardScoringConfig::class.java
                )
                val clocLangList = copyLines.filter {
                    standardScoringConfig.clocLanguage.contains(it.key)
                }
                if (clocLangList.isNotEmpty()) {
                    standardScoringConfig.lineCount = clocLangList.map { it.value }.sum()
                    scoringConfigs[lang] = standardScoringConfig
                    clocLangList.forEach { (t, _) ->
                        copyLines.remove(t)
                    }
                }
                iterator.remove()
            } else {
                logger.error("initScoringConfigs standard scoring config is null, " +
                        "task id: ${taskDetailVO.taskId}, ${lang.langName()}")
            }
        }

        // 当用户选了七种语言之外的语言并且cloc中有七种语言之外的数据时，把这些数据统一到 Others 中
        if (languageList.size > 0 && copyLines.isNotEmpty()) {
            val othersConfig = StandardScoringConfig()
            languageList.forEach { lang ->
                othersConfig.clocLanguage.add(lang.langName())
            }
            othersConfig.lineCount = copyLines.map { it.value }.sum()
            scoringConfigs[ComConstants.CodeLang.OTHERS] = othersConfig
        }

        return scoringConfigs
    }

    /**
     * 获取任务对应代码行告警
     * 按语言区分
     * @param taskId
     * @param buildId
     */
    fun getCLOCDefectNum(taskId: Long, toolName: String, buildId: String): MutableMap<String, Long> {
        val res = clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId)
        val pairs = mutableMapOf<String, Long>()
        res.forEach {
            val totalLine = it.sumCode
            pairs[it.language] = totalLine
        }
        return pairs
    }
}
