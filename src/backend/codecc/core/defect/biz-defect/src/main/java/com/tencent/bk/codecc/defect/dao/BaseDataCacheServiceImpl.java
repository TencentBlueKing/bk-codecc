package com.tencent.bk.codecc.defect.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.constant.DefectMessageCode;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BaseDataCacheService;
import com.tencent.devops.common.codecc.util.JsonUtil;
import java.util.ArrayList;
import java.util.Collections;
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


    private static final String PARAMS_TYPE_HISTORY_IGNORE_TYPE = "HISTORY_IGNORE_TYPE";


    /**
     * 工具基础信息缓存
     */
    private final Map<String, BaseDataVO> baseDataVOMap = Maps.newConcurrentMap();


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
    public List<BaseDataVO> getLanguageBaseDataFromCache(List<String> languages) {
        if (baseDataVOMap.isEmpty()) {
            initMap();
        }

        List<BaseDataVO> baseDataVOList = new ArrayList<>();
        languages.forEach(lang -> {
            baseDataVOMap.values().forEach(baseDataVO -> {
                List<String> langArray = JsonUtil.INSTANCE.to(baseDataVO.getParamExtend2(),
                        new TypeReference<List<String>>() {});
                if (langArray.contains(lang)) {
                    baseDataVOList.add(baseDataVO);
                }
            });
        });

        return baseDataVOList;
    }

    @Override
    public List<BaseDataVO> getLanguageBaseDataFromCache(Long codeLang) {
        if (baseDataVOMap.isEmpty()) {
            initMap();
        }

        return baseDataVOMap.values().stream()
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
        if (baseDataVOMap.isEmpty()) {
            initMap();
        }

        return baseDataVOMap.get(ComConstants.KEY_TOOL_ORDER);
    }

    @Override
    public int getMaxBuildListSize() {
        if (baseDataVOMap.isEmpty() || baseDataVOMap.get(ComConstants.MAX_BUILD_LIST_SIZE) == null) {
            initMap();
        }

        BaseDataVO baseDataVO = new BaseDataVO();
        baseDataVO.setParamValue("500");
        return Integer.parseInt(baseDataVOMap.getOrDefault(ComConstants.MAX_BUILD_LIST_SIZE, baseDataVO)
                .getParamValue());
    }

    @Override
    public Integer getHistoryIgnoreType() {
        try {
            List<BaseDataVO> baseDataVOS = typeToDetailCache.get(PARAMS_TYPE_HISTORY_IGNORE_TYPE);
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

    private void initMap() {
        Result<List<BaseDataVO>> result = client.get(ServiceBaseDataResource.class).findBaseData();
        if (result.isNotOk() || result.getData() == null || result.getData().isEmpty()) {
            log.error("all tool base data is null");
            throw new CodeCCException(DefectMessageCode.BASE_DATA_NOT_FOUND);
        }
        baseDataVOMap.clear();
        result.getData().forEach(baseDataVO -> {
            if (StringUtils.isNotBlank(baseDataVO.getLangFullKey())) {
                baseDataVOMap.put(baseDataVO.getLangFullKey(), baseDataVO);
            } else {
                baseDataVOMap.put(baseDataVO.getParamCode(), baseDataVO);
            }
        });
    }
}
