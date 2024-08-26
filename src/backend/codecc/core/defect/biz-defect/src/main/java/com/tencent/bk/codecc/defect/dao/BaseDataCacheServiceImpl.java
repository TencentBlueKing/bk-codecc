package com.tencent.bk.codecc.defect.dao;

import static com.tencent.devops.common.constant.ComConstants.HISTORY_IGNORE_TYPE;
import static com.tencent.devops.common.constant.ComConstants.METADATA_TYPE_LANG;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BaseDataCacheService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BaseDataCacheServiceImpl implements BaseDataCacheService {

    @Autowired
    private Client client;
    private LoadingCache<String, List<BaseDataVO>> typeToDetailCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, List<BaseDataVO>>() {
                @Override
                public List<BaseDataVO> load(@NotNull String key) {
                    try {
                        Result<List<BaseDataVO>> result =
                                client.get(ServiceBaseDataResource.class).getParamsByType(key);
                        if (result.isNotOk() || result.getData() == null || result.getData().isEmpty()) {
                            return Collections.emptyList();
                        }
                        return result.getData();
                    } catch (Exception e) {
                        return Collections.emptyList();
                    }
                }
            });

    @Override
    public BaseDataVO getByTypeAndCode(String type, String code) {
        List<BaseDataVO> baseDataVOS = typeToDetailCache.getUnchecked(type);
        if (CollectionUtils.isEmpty(baseDataVOS)) {
            return null;
        }
        for (BaseDataVO dataVO : baseDataVOS) {
            if (dataVO != null && StringUtils.isNotEmpty(dataVO.getParamCode())
                    && dataVO.getParamCode().equals(code)) {
                return dataVO;
            }
        }
        return null;
    }

    @Override
    public List<BaseDataVO> getByType(String type) {
        return typeToDetailCache.getUnchecked(type);

    }

    @Override
    public List<BaseDataVO> getLanguageBaseDataFromCache(Long codeLang) {
        List<BaseDataVO> baseDataVOS = typeToDetailCache.getUnchecked(ComConstants.KEY_CODE_LANG);
        if (CollectionUtils.isEmpty(baseDataVOS)) {
            return Collections.emptyList();
        }
        return baseDataVOS.stream()
                .filter(baseDataVO -> {
                    if (StringUtils.isNumeric(baseDataVO.getParamCode())) {
                        long paramCode = Long.parseLong(baseDataVO.getParamCode());
                        return (paramCode & codeLang) != 0;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    @Override
    public BaseDataVO getToolOrder() {
        List<BaseDataVO> baseDataVOS = typeToDetailCache.getUnchecked(ComConstants.KEY_TOOL_ORDER);
        if (CollectionUtils.isEmpty(baseDataVOS)) {
            return null;
        }
        return baseDataVOS.get(0);
    }

    @Override
    public int getMaxBuildListSize() {
        List<BaseDataVO> baseDataVOS = typeToDetailCache.getUnchecked(ComConstants.MAX_BUILD_LIST_SIZE);
        if (CollectionUtils.isEmpty(baseDataVOS)) {
            return 500;
        }
        return Integer.parseInt(baseDataVOS.get(0).getParamValue());
    }

    @Override
    public Integer getHistoryIgnoreType() {
        try {
            List<BaseDataVO> baseDataVOS = typeToDetailCache.get(HISTORY_IGNORE_TYPE);
            if (CollectionUtils.isEmpty(baseDataVOS)) {
                log.error("getHistoryIgnoreType is empty");
                return null;
            }
            BaseDataVO baseDataVO = baseDataVOS.get(0);
            return StringUtils.isNotBlank(baseDataVO.getParamValue())
                    ? Integer.parseInt(baseDataVO.getParamValue()) : null;
        } catch (Exception e) {
            log.error("getHistoryIgnoreType fail!", e);
        }
        return null;
    }

    @Override
    public Map<String, Long> getLangToValueMap() {
        try {
            List<BaseDataVO> langMetas = getByType(METADATA_TYPE_LANG);
            if (CollectionUtils.isEmpty(langMetas)) {
                log.error("getLangToValueMap is empty");
                return Collections.emptyMap();
            }
            Map<String, Long> langToValueMap = new HashMap<>();
            for (BaseDataVO langMeta : langMetas) {
                if (StringUtils.isBlank(langMeta.getParamExtend2()) || StringUtils.isBlank(langMeta.getParamCode())
                        || !StringUtils.isNumeric(langMeta.getParamCode())) {
                    continue;
                }
                Long langValue = Long.valueOf(langMeta.getParamCode());
                JsonUtil.INSTANCE.to(langMeta.getParamExtend2(), new TypeReference<List<String>>() {
                        }).forEach(it -> langToValueMap.put(it, langValue));
            }
            return langToValueMap;
        } catch (Exception e) {
            log.error("getLangToValueMap fail!", e);
        }
        return Collections.emptyMap();
    }
}
