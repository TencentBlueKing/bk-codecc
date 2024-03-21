package com.tencent.devops.common.service;

import com.tencent.devops.common.api.BaseDataVO;
import java.util.List;

public interface BaseDataCacheService {

    BaseDataVO getByTypeAndCode(String type, String code);

    List<BaseDataVO> getByType(String type);

    List<BaseDataVO> getLanguageBaseDataFromCache(Long codeLang);

    BaseDataVO getToolOrder();

    int getMaxBuildListSize();

    /**
     * 获取存量告警的忽略类型
     * @return
     */
    Integer getHistoryIgnoreType();
}
