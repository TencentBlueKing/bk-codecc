package com.tencent.bk.codecc.defect.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.codecc.task.api.ServiceDynamicConfigResource;
import com.tencent.devops.common.api.DynamicConfigVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DynamicConfigCache {

    @Autowired
    private Client client;

    private static final Logger logger = LoggerFactory.getLogger(DynamicConfigCache.class);


    private final LoadingCache<String, String> cache = CacheBuilder.newBuilder().maximumSize(10)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(String s) throws Exception {
                    return loadDynamicConfigValue(s);
                }
            });


    private String loadDynamicConfigValue(String key) {
        Result<DynamicConfigVO> vo = client.get(ServiceDynamicConfigResource.class)
                .getConfigByKey(key);
        return vo == null || vo.getData() == null || vo.getData().getValue() == null ? "" : vo.getData().getValue();
    }

    public String getConfigValueFromCache(ComConstants.DynamicConfigKey key) {
        String value = null;
        try {
            value = cache.get(key.getKey());
        } catch (Exception e) {
            logger.error("getConfigValueFromCache Cause Error. key:" + key, e);
        }
        return value != null ? value : key.getDefaultValue();
    }




}
