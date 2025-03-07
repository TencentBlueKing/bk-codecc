package com.tencent.bk.codecc.defect.service.impl.sca

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CCNStatisticRepository
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CLOCStatisticRepository
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CommonStatisticRepository
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.DUPCStatisticRepository
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.LintStatisticRepository
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.statistic.SCAStatisticRepository
import com.tencent.bk.codecc.defect.model.statistic.StatisticEntity
import com.tencent.bk.codecc.defect.service.impl.AbstractClusterDefectService
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO
import com.tencent.devops.common.api.clusterresult.SCAClusterResultVO
import com.tencent.devops.common.constant.ComConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("SCA")
class SCAClusterDefectServiceImpl @Autowired constructor(
    lintStatisticRepository: LintStatisticRepository,
    commonStatisticRepository: CommonStatisticRepository,
    ccnStatisticRepository: CCNStatisticRepository,
    clocStatisticRepository: CLOCStatisticRepository,
    dupcStatisticRepository: DUPCStatisticRepository,
    private val scaStatisticRepository: SCAStatisticRepository
) : AbstractClusterDefectService(
    lintStatisticRepository,
    commonStatisticRepository,
    dupcStatisticRepository,
    ccnStatisticRepository,
    clocStatisticRepository
) {
    /**
     * SCA工具不参与开源治理度量，没有聚类逻辑
     */
    override fun cluster(
        taskId: Long,
        buildId: String,
        toolList: List<String>,
        isMigrationSuccessful: Boolean,
        toolNameToDimensionStatisticMap: Map<String, StatisticEntity>
    ) {
        logger.info("sca cluster $taskId $buildId ${toolList.size}")
    }

    override fun getClusterStatistic(taskId: Long, buildId: String): BaseClusterResultVO {
        val scaClusterResultVO = SCAClusterResultVO()
        scaClusterResultVO.type = ComConstants.ToolType.SCA.name
        // TODO 当前设计仅计算一个工具，后续SCA支持多工具设计时，需要聚合展示（不能简单多工具相加）
        val scaClusterStatisticEntity =
            scaStatisticRepository.findFirstByTaskIdAndBuildId(taskId, buildId)
                ?: return scaClusterResultVO
        scaClusterResultVO.totalCount = scaClusterStatisticEntity.defectCount
        scaClusterResultVO.packageCount = scaClusterStatisticEntity.packageCount
        scaClusterResultVO.newVulCount = scaClusterStatisticEntity.newVulCount
        scaClusterResultVO.newHighVulCount = scaClusterStatisticEntity.newHighVulCount
        scaClusterResultVO.newMediumVulCount = scaClusterStatisticEntity.newMediumVulCount
        scaClusterResultVO.newLowVulCount = scaClusterStatisticEntity.newLowVulCount
        scaClusterResultVO.licenseCount = scaClusterStatisticEntity.licenseCount
        scaClusterResultVO.type = ComConstants.ToolType.SCA.name
        scaClusterResultVO.toolNum = 1
        scaClusterResultVO.toolList = listOf(ComConstants.Tool.SCA.name)
        return scaClusterResultVO
    }

    fun getLatestClusterStatistic(taskId: Long): BaseClusterResultVO {
        val scaClusterResultVO = SCAClusterResultVO()
        scaClusterResultVO.type = ComConstants.ToolType.SCA.name
        val latestStatisticEntity =
            scaStatisticRepository.findTopByTaskIdOrderByTimeDesc(taskId)
                ?: return scaClusterResultVO
        scaClusterResultVO.totalCount = latestStatisticEntity.defectCount
        scaClusterResultVO.packageCount = latestStatisticEntity.packageCount
        scaClusterResultVO.newVulCount = latestStatisticEntity.newVulCount
        scaClusterResultVO.newHighVulCount = latestStatisticEntity.newHighVulCount
        scaClusterResultVO.newMediumVulCount = latestStatisticEntity.newMediumVulCount
        scaClusterResultVO.newLowVulCount = latestStatisticEntity.newLowVulCount
        scaClusterResultVO.licenseCount = latestStatisticEntity.licenseCount
        scaClusterResultVO.type = ComConstants.ToolType.SCA.name
        scaClusterResultVO.toolNum = 1
        scaClusterResultVO.toolList = listOf(ComConstants.Tool.SCA.name)
        return scaClusterResultVO
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SCAClusterDefectServiceImpl::class.java)
    }
}
