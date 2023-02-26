package com.tencent.bk.codecc.codeccjob.service.impl

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource
import com.tencent.devops.common.api.BaseDataVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.codecc.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class CCNDefectService @Autowired constructor(
    private val client: Client
) {

    private val logger = LoggerFactory.getLogger(CCNDefectService::class.java)


    private val riskFactorConfigCache: LoadingCache<String, Map<String, String>> = CacheBuilder.newBuilder()
        .maximumSize(10)
        .refreshAfterWrite(10, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, Map<String, String>>() {
            override fun load(toolName: String): Map<String, String> {
                return getRiskFactorConfig(toolName) ?:
                    throw CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, arrayOf("风险系数"), "")
            }
        })


    /**
     * 根据风险系数配置给告警方法赋值风险系数
     */
    fun fillingRiskFactor(ccnDefectEntityList: List<CCNDefectEntity>): List<CCNDefectEntity> {
        if (ccnDefectEntityList.isEmpty()) {
            return ccnDefectEntityList
        }

        val riskFactorConfMap = riskFactorConfigCache.get(ComConstants.Tool.CCN.name)
        val sh = Integer.valueOf(riskFactorConfMap[ComConstants.RiskFactor.SH.name])
        val h = Integer.valueOf(riskFactorConfMap[ComConstants.RiskFactor.H.name])
        val m = Integer.valueOf(riskFactorConfMap[ComConstants.RiskFactor.M.name])

        ccnDefectEntityList.forEach { ccnDefectEntity ->
            val ccn = ccnDefectEntity.ccn
            if (ccn >= m && ccn < h) {
                ccnDefectEntity.riskFactor = ComConstants.RiskFactor.M.value()
            } else if (ccn >= h && ccn < sh) {
                ccnDefectEntity.riskFactor = ComConstants.RiskFactor.H.value()
            } else if (ccn >= sh) {
                ccnDefectEntity.riskFactor = ComConstants.RiskFactor.SH.value()
            } else if (ccn < m) {
                ccnDefectEntity.riskFactor = ComConstants.RiskFactor.L.value()
            }
        }

        return ccnDefectEntityList
    }

    /**
     * 获取圈复杂风险函数值
     *
     * @param ccn 圈复杂度值
     * @return
     */
    fun getRiskFactor(ccn: Int): Int {
        val riskFactorConfMap: Map<String, String> = riskFactorConfigCache.get(ComConstants.Tool.CCN.name)
        val sh = Integer.valueOf(riskFactorConfMap[ComConstants.RiskFactor.SH.name])
        val h = Integer.valueOf(riskFactorConfMap[ComConstants.RiskFactor.H.name])
        val m = Integer.valueOf(riskFactorConfMap[ComConstants.RiskFactor.M.name])
        if (ccn >= m && ccn < h) {
            return ComConstants.RiskFactor.M.value()
        } else if (ccn >= h && ccn < sh) {
            return ComConstants.RiskFactor.H.value()
        } else if (ccn >= sh) {
            return ComConstants.RiskFactor.SH.value()
        } else if (ccn < m) {
            return ComConstants.RiskFactor.L.value()
        }
        return ComConstants.RiskFactor.SH.value()
    }

    private fun getRiskFactorConfig(toolName: String): Map<String, String>? {
        //获取风险系数值
        val baseDataResult: Result<List<BaseDataVO>?> = client.get(ServiceBaseDataResource::class.java)
            .getInfoByTypeAndCode(ComConstants.PREFIX_RISK_FACTOR_CONFIG, toolName)
        if (baseDataResult.isNotOk() || null == baseDataResult.data) {
            logger.error("get risk coefficient fail!")
            throw CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL)
        }
        return baseDataResult.data!!.map { it.paramName to it.paramValue }.toMap()
    }
}
