package com.tencent.bk.codecc.defect.service.specialparam;

import com.tencent.devops.common.service.BizServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 特殊参数工具类
 *
 * @version V4.0
 * @date 2019/3/12
 */
@Service
public class SpecialParamUtil {
    private static final String BUSINESS_TYPE = "SpecialParam";

    @Autowired
    private BizServiceFactory<ISpecialParamService> specialParamServiceBizServiceFactory;

    /**
     * 特殊参数是否相同
     *
     * @param toolName
     * @param paramJson1
     * @param paramJson2
     */
    public boolean isSameParam(String toolName, String paramJson1, String paramJson2) {
        if (StringUtils.isEmpty(paramJson1)) {
            paramJson1 = new JSONObject().toString();
        }
        if (StringUtils.isEmpty(paramJson2)) {
            paramJson2 = new JSONObject().toString();
        }
        ISpecialParamService processor = specialParamServiceBizServiceFactory.createBizService(toolName, BUSINESS_TYPE, ISpecialParamService.class);
        return processor.isSameParam(toolName, paramJson1, paramJson2);
    }

}
