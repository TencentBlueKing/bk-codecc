package com.tencent.bk.codecc.defect.utils;

import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CCNUtils {

    private static Logger logger = LoggerFactory.getLogger(CCNUtils.class);

    private static Map<String, String> riskFactorConfMap = null;


    /**
     * 填充告警实体的圈复杂风险函数值
     *
     * @param ccnDefectEntity
     * @param riskFactorConfMap
     * @return
     */
    public static void fillingRiskFactor(CCNDefectEntity ccnDefectEntity, Map<String, String> riskFactorConfMap) {
        if (riskFactorConfMap == null) {
            logger.error("Has not init risk factor config!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"风险系数"}, null);
        }
        int sh = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.SH.name()));
        int h = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.H.name()));
        int m = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.M.name()));

        int ccn = ccnDefectEntity.getCcn();
        if (ccn >= m && ccn < h) {
            ccnDefectEntity.setRiskFactor(ComConstants.RiskFactor.M.value());
        } else if (ccn >= h && ccn < sh) {
            ccnDefectEntity.setRiskFactor(ComConstants.RiskFactor.H.value());
        } else if (ccn >= sh) {
            ccnDefectEntity.setRiskFactor(ComConstants.RiskFactor.SH.value());
        } else if (ccn < m) {
            ccnDefectEntity.setRiskFactor(ComConstants.RiskFactor.L.value());
        }
    }

    /**
     * 获取圈复杂风险函数值
     *
     * @param ccn 圈复杂度值
     * @return
     */
    public static int getRiskFactor(int ccn) {
        Map<String, String> riskFactorConfMap = getRiskFactoryMap();
        int sh = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.SH.name()));
        int h = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.H.name()));
        int m = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.M.name()));

        if (ccn >= m && ccn < h) {
            return ComConstants.RiskFactor.M.value();
        } else if (ccn >= h && ccn < sh) {
            return ComConstants.RiskFactor.H.value();
        } else if (ccn >= sh) {
            return ComConstants.RiskFactor.SH.value();
        } else if (ccn < m) {
            return ComConstants.RiskFactor.L.value();
        }
        return ComConstants.RiskFactor.SH.value();
    }

    private static synchronized Map<String, String> getRiskFactoryMap() {
        if (riskFactorConfMap != null) {
            return riskFactorConfMap;
        }
        riskFactorConfMap = SpringContextUtil.Companion.getBean(ThirdPartySystemCaller.class)
            .getRiskFactorConfig(ComConstants.Tool.CCN.name());
        return riskFactorConfMap;
    }

}
