package com.tencent.bk.codecc.defect.service.specialparam;

import com.tencent.devops.common.constant.ComConstants;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

/**
 * PHPCS特殊参数处理器实现类
 *
 * @version V4.0
 * @date 2019/3/12
 */
@Service("PHPCSSpecialParamBizService")
public class PHPCSSpecialParamServiceImpl extends AbstractSpecialParamServiceImpl {

    /**
     * 特殊参数是否相同
     *
     * @param toolName
     * @param paramJson1
     * @param paramJson2
     */
    @Override
    public boolean isSameParam(String toolName, String paramJson1, String paramJson2) {
        JSONObject paramJsonObj1 = new JSONObject(paramJson1);
        JSONObject paramJsonObj2 = new JSONObject(paramJson2);
        return isSameParam(paramJsonObj1, paramJsonObj2, ComConstants.KEY_PHPCS_STANDARD);
    }

}
