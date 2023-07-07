package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.cache.DynamicConfigCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.DynamicConfigKey.EFFECTIVE_COMMENT_FILTER_LANGS;


/**
 * 处理有效注释逻辑，包含过滤，与展示
 */
@Service
public class EfficientCommentService {

    @Autowired
    private DynamicConfigCache dynamicConfigCache;

    public boolean checkIfShowEffectiveComment(String lang) {
        return checkIfShowEffectiveComment(getDisableLangList(), lang);
    }

    public boolean checkIfShowEffectiveComment(List<String> filters, String lang) {
        if (CollectionUtils.isEmpty(filters) || StringUtils.isEmpty(lang)) {
            return true;
        }
        return !filters.contains(lang.toLowerCase().trim());
    }


    public List<String> getDisableLangList() {
        String config =
                dynamicConfigCache.getConfigValueFromCache(EFFECTIVE_COMMENT_FILTER_LANGS);
        if (StringUtils.isEmpty(config)) {
            return Collections.emptyList();
        }
        List<String> filters = Arrays.asList(config.toLowerCase().split(","));
        filters = filters.stream().map(String::trim).collect(Collectors.toList());
        return filters;
    }
}
