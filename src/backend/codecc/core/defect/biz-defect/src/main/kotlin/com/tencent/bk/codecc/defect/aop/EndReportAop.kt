package com.tencent.bk.codecc.defect.aop

import com.tencent.bk.codecc.defect.pojo.HandlerDTO
import com.tencent.bk.codecc.defect.service.impl.handler.SaveSnapshotSummaryHandler
import com.tencent.bk.codecc.defect.vo.CommitDefectVO
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO
import com.tencent.devops.common.service.ToolMetaCacheService
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Aspect
@Component
class EndReportAop @Autowired constructor(
    private val toolMetaCacheService: ToolMetaCacheService,
    private val saveSnapshotSummaryHandler: SaveSnapshotSummaryHandler
) {

    companion object {
        private val logger = LoggerFactory.getLogger(EndReportAop::class.java)
    }

    @Pointcut("@annotation(com.tencent.devops.common.web.aop.annotation.EndReport)")
    fun endReport() {
    }

    /**
     * 普通告警上报后续处理逻辑：
     * 1. 构建快照生成
     * 2. 按告警类型聚类统计信息
     * 3. 度量分数计算
     * @param commitDefectVO 告警上报信息，从中获取任务ID和构建号
     */
    @After("endReport()&&args(commitDefectVO)")
    fun scoring(commitDefectVO: CommitDefectVO) {
        logger.info(
            "begin to commit handler: ${commitDefectVO.taskId} " +
                    "${commitDefectVO.buildId} ${commitDefectVO.toolName}"
        )
        val handlerDTO = generateHandlerDTO(commitDefectVO, null)
        saveSnapshotSummaryHandler.handler(handlerDTO)
    }

    /**
     * 超快增量告警上报后续处理逻辑：
     * 1. 构建快照生成
     * 2. 按告警类型聚类统计信息
     * 3. 度量分数计算
     * @param analyzeConfigInfoVO 分析配置信息，从中获取任务ID和构建号
     */
    @After("endReport()&&args(analyzeConfigInfoVO)")
    fun scoring(analyzeConfigInfoVO: AnalyzeConfigInfoVO) {
        logger.info(
            "begin to commit handler by FI: ${analyzeConfigInfoVO.taskId} " +
                    "${analyzeConfigInfoVO.buildId} ${analyzeConfigInfoVO.multiToolType}"
        )
        val handlerDTO = generateHandlerDTO(null, analyzeConfigInfoVO)
        saveSnapshotSummaryHandler.handler(handlerDTO)
    }

    private fun generateHandlerDTO(
        commitDefectVO: CommitDefectVO?,
        analyzeConfigInfoVO: AnalyzeConfigInfoVO?
    ): HandlerDTO {
        return HandlerDTO(
            toolMetaCacheService.getToolPattern(
                commitDefectVO?.toolName ?: analyzeConfigInfoVO?.multiToolType
            )
        ).apply {
            if (commitDefectVO != null) {
                BeanUtils.copyProperties(commitDefectVO, this)
            }

            if (analyzeConfigInfoVO != null) {
                BeanUtils.copyProperties(analyzeConfigInfoVO, this)
                this.toolName = analyzeConfigInfoVO.multiToolType
            }
        }
    }
}
