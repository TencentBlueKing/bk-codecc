package com.tencent.bk.codecc.task.component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.devops.common.constant.ComConstants;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * base data 统一缓存
 */
@Slf4j
@Component
public class BaseDataCommonCache {

    @Autowired
    private BaseDataRepository baseDataRepository;

    /**
     * 默认项目
     */
    private LoadingCache<String, Integer> defaultTaskLimitCache = CacheBuilder.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Integer>() {
                @Override
                public Integer load(String s) {
                    try {
                        BaseDataEntity baseDataEntity = baseDataRepository.findFirstByParamTypeAndParamCode(
                                ComConstants.KEY_PIPELINE_TASK_LIMIT,
                                ComConstants.DEFAULT_PIPELINE_TASK_LIMIT_KEY);
                        if (baseDataEntity != null) {
                            return StringUtils.isNotBlank(baseDataEntity.getParamValue())
                                    ? Integer.valueOf(baseDataEntity.getParamValue()) :
                                    ComConstants.DEFAULT_PIPELINE_TASK_LIMIT_VALUE;
                        }
                        return ComConstants.DEFAULT_PIPELINE_TASK_LIMIT_VALUE;
                    } catch (Exception e) {
                        log.error("get default pipeline task limit fails return default 50", e);
                        return ComConstants.DEFAULT_PIPELINE_TASK_LIMIT_VALUE;
                    }
                }
            });


    public Integer getDefaultPipelineTaskLimit() {
        try {
            return defaultTaskLimitCache.get(ComConstants.DEFAULT_PIPELINE_TASK_LIMIT_KEY);
        } catch (Exception e) {
            log.error("get default pipeline task limit fails return default", e);
            return ComConstants.DEFAULT_PIPELINE_TASK_LIMIT_VALUE;
        }
    }


}
