package com.tencent.bk.codecc.task.service.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource;
import com.tencent.bk.codecc.defect.vo.checkerset.CheckerSetPackageVO;
import com.tencent.bk.codecc.task.service.BaseDataService;
import com.tencent.bk.codecc.task.service.CheckerSetPackageCacheService;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.OrgInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CheckerSetPackageCacheServiceImpl implements CheckerSetPackageCacheService {

    @Autowired
    private Client client;

    @Autowired
    private BaseDataService baseDataService;

    /**
     * 规则包缓存
     * LoadingCache Key -> LangValue
     * Map Key -> type
     */
    private LoadingCache<Long, Map<String, List<CheckerSetPackageVO>>> packageCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, Map<String, List<CheckerSetPackageVO>>>() {
                @Override
                public Map<String, List<CheckerSetPackageVO>> load(@NotNull Long langValue) {
                    if (langValue == 0L) {
                        return Collections.emptyMap();
                    }
                    List<CheckerSetPackageVO> packageVOS = getPackageByLangValue(langValue);
                    if (CollectionUtils.isEmpty(packageVOS)) {
                        return Collections.emptyMap();
                    }
                    return packageVOS.stream().collect(Collectors.groupingBy(CheckerSetPackageVO::getType));
                }
            });


    private List<CheckerSetPackageVO> getPackageByLangValue(Long langValue) {
        try {
            Result<List<CheckerSetPackageVO>> result = client.get(ServiceCheckerSetRestResource.class)
                    .getPackageByLangValue(langValue);
            return result == null || result.isNotOk() ? Collections.emptyList() : result.getData();
        } catch (Exception e) {
            log.error("get checker set package cause erro langValue:" + langValue, e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<CheckerSetPackageVO> getPackageByLangValueFromCache(Long langValue) {
        if (langValue == null) {
            return Collections.emptyList();
        }
        List<CheckerSetPackageVO> packageVOS = new ArrayList<>();
        for (List<CheckerSetPackageVO> value : packageCache.getUnchecked(langValue).values()) {
            packageVOS.addAll(value);
        }
        return packageVOS;
    }

    @Override
    public Map<Long, List<CheckerSetPackageVO>> getPackageByLangValueFromCache(List<Long> langValues) {
        if (CollectionUtils.isEmpty(langValues)) {
            return Collections.emptyMap();
        }
        Map<Long, List<CheckerSetPackageVO>> langValueMap = new HashMap<>();
        for (Long langValue : langValues) {
            if (langValue != null) {
                langValueMap.put(langValue, getPackageByLangValueFromCache(langValue));
            }
        }
        return langValueMap;
    }

    @Override
    public List<CheckerSetPackageVO> getPackageByLangValueAndTypeFromCache(Long langValue, String type) {
        if (langValue == null) {
            return Collections.emptyList();
        }
        return packageCache.getUnchecked(langValue).getOrDefault(type, Collections.emptyList());
    }

    @Override
    public Map<Long, List<CheckerSetPackageVO>> getPackageByLangValueAndTypeFromCache(List<Long> langValues,
            String type) {
        if (CollectionUtils.isEmpty(langValues)) {
            return Collections.emptyMap();
        }
        Map<Long, List<CheckerSetPackageVO>> langValueMap = new HashMap<>();
        for (Long langValue : langValues) {
            if (langValue != null) {
                langValueMap.put(langValue, getPackageByLangValueAndTypeFromCache(langValue, type));
            }
        }
        return langValueMap;
    }

    @Override
    public List<CheckerSetPackageVO> getPackageByTypeFromCache(String type) {
        List<BaseDataVO> langBaseDataList = baseDataService.findBaseDataInfoByType(ComConstants.KEY_CODE_LANG);
        if (CollectionUtils.isEmpty(langBaseDataList)) {
            return Collections.emptyList();
        }
        List<CheckerSetPackageVO> packageVOS = new ArrayList<>();
        for (BaseDataVO langBaseData : langBaseDataList) {
            if (StringUtils.isNumeric(langBaseData.getParamValue())) {
                packageVOS.addAll(getPackageByLangValueFromCache(Long.valueOf(langBaseData.getParamValue())));
            }
        }
        return packageVOS;
    }

    @Override
    public List<CheckerSetPackageVO> getPackageByLangValueAndTypeAndEnvTypeAndScopesFromCache(Long langValue,
            String type, String envType, OrgInfoVO orgInfo, BsTaskCreateFrom createFrom) {
        List<CheckerSetPackageVO> packageVOS = getPackageByLangValueAndTypeFromCache(langValue, type);
        if (CollectionUtils.isEmpty(packageVOS)) {
            return Collections.emptyList();
        }
        return packageVOS.stream().filter(it -> it.getEnvType().equals(envType)).filter(it ->
                        CollectionUtils.isEmpty(it.getScopes())
                                || it.getScopes().stream().anyMatch(org -> org.contains(orgInfo)))
                .filter(it -> CollectionUtils.isEmpty(it.getTaskCreateFromScopes())
                        || it.getTaskCreateFromScopes().contains(createFrom.value()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CheckerSetPackageVO> getPackageByLangValueAndTypeAndEnvTypeFromCache(Long langValue,
            String type, String envType) {
        List<CheckerSetPackageVO> packageVOS = getPackageByLangValueAndTypeFromCache(langValue, type);
        if (CollectionUtils.isEmpty(packageVOS)) {
            return Collections.emptyList();
        }
        return packageVOS.stream().filter(packageVO -> StringUtils.isBlank(envType)
                        || envType.equals(packageVO.getEnvType()))
                .collect(Collectors.toList());
    }
}
