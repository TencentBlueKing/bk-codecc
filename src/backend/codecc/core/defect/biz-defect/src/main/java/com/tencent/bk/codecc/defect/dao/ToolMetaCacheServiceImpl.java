/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.ServiceToolMetaRestResource;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.ToolOption;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.ToolMetaCacheService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 工具缓存
 *
 * @version V1.0
 * @date 2019/3/7
 */
@Slf4j
@Component
public class ToolMetaCacheServiceImpl implements ToolMetaCacheService {
    @Autowired
    private Client client;

    /**
     * 工具基础信息缓存
     */
    private Map<String, ToolMetaBaseVO> toolMetaBasicMap = Maps.newConcurrentMap();

    /**
     * 工具维度基础信息缓存
     */
    private Map<String, Set<ToolMetaBaseVO>> toolMetaBasicDimensionMap = Maps.newConcurrentMap();

    /**
     * 工具国际化展示名字缓存
     * map结构: {local,{toolName,displayName}}
     */
    private Map<String, Map<String, String>> TOOL_DISPLAY_NAME_MAP = Maps.newConcurrentMap();

    /**
     * 加载工具缓存
     */
    @Override
    public List<ToolMetaBaseVO> loadToolBaseCache() {
        Result<Map<String, ToolMetaBaseVO>> taskResult =
                client.get(ServiceTaskRestResource.class).getToolMetaListFromCache();
        if (taskResult.isNotOk() || null == taskResult.getData() || MapUtils.isEmpty(taskResult.getData())) {
            log.error("all tool metadata is empty!");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        Map<String, ToolMetaBaseVO> toolMetaBaseVOMap = taskResult.getData();

        toolMetaBasicMap.clear();
        toolMetaBasicDimensionMap.clear();
        List<ToolMetaBaseVO> toolMetaBaseVOS = Lists.newArrayList();
        for (Map.Entry<String, ToolMetaBaseVO> entry : toolMetaBaseVOMap.entrySet()) {
            // 缓存基础信息
            ToolMetaBaseVO tool = entry.getValue();
            toolMetaBasicMap.put(tool.getName(), tool);
            toolMetaBaseVOS.add(tool);

            // 缓存维度基础信息
            // DEFECT类型的工具特殊处理下
            String dimensionMapKey = tool.getType();
            if (dimensionMapKey.equals(ComConstants.ToolType.DEFECT.name())
                    && tool.getPattern().equals(ComConstants.ToolPattern.LINT.name())) {
                dimensionMapKey = ComConstants.ToolPattern.LINT.name();
            }
            Set<ToolMetaBaseVO> toolDimensionSet = toolMetaBasicDimensionMap.get(dimensionMapKey);
            if (toolDimensionSet == null) {
                toolDimensionSet = new CopyOnWriteArraySet<>();
            }
            toolDimensionSet.add(tool);
            toolMetaBasicDimensionMap.put(dimensionMapKey, toolDimensionSet);
        }

        log.info("load tool dimension cache success: {}", toolMetaBasicDimensionMap.size());
        log.info("load tool cache success: {}", toolMetaBasicMap.size());

        return toolMetaBaseVOS;
    }

    /**
     * 加载工具缓存
     */
    @Override
    public List<ToolMetaDetailVO> loadToolDetailCache() {
        return null;
    }

    /**
     * 根据工具名获取工具模型
     *
     * @param toolName
     * @return
     */
    @Override
    public String getToolPattern(String toolName) {
        String pattern;
        if (toolMetaBasicMap.get(toolName) != null
                && StringUtils.isNotEmpty(toolMetaBasicMap.get(toolName).getPattern())) {
            pattern = toolMetaBasicMap.get(toolName).getPattern();
        } else {
            ToolMetaBaseVO toolMetaBaseVO = getToolFromCache(toolName);
            pattern = toolMetaBaseVO.getPattern();
        }
        return pattern;
    }

    /**
     * 查询工具执行聚类逻辑的类型
     *
     * @param toolName
     * @return
     */
    @Override
    public String getClusterType(String toolName) {
        String clusterType;
        if (toolMetaBasicMap.get(toolName) != null
                && StringUtils.isNotEmpty(toolMetaBasicMap.get(toolName).getClusterType())) {
            clusterType = toolMetaBasicMap.get(toolName).getClusterType();
        } else {
            ToolMetaBaseVO toolMetaBaseVO = getToolFromCache(toolName);
            clusterType = toolMetaBaseVO.getClusterType();
        }
        return clusterType;
    }

    /**
     * 根据工具名获取工具模型
     *
     * @param toolName
     * @return
     */
    public String getToolParams(String toolName) {
        String params;
        if (toolMetaBasicMap.get(toolName) != null
                && StringUtils.isNotEmpty(toolMetaBasicMap.get(toolName).getParams())) {
            params = toolMetaBasicMap.get(toolName).getParams();
        } else {
            ToolMetaBaseVO toolMetaBaseVO = getToolFromCache(toolName);
            params = toolMetaBaseVO.getParams();
        }
        return params;
    }

    /**
     * 获取工具基础信息缓存
     *
     * @param toolName
     * @return
     */
    @Override
    public ToolMetaBaseVO getToolBaseMetaCache(String toolName) {
        if (toolMetaBasicMap.get(toolName) != null) {
            return toolMetaBasicMap.get(toolName);
        } else {
            ToolMetaBaseVO toolMetaBaseVO = getToolFromCache(toolName);
            return toolMetaBaseVO;
        }
    }

    /**
     * 获取工具显示名称
     *
     * @param toolName
     * @return
     */
    @Override
    public String getToolDisplayName(String toolName) {
        String displayName;
        if (toolMetaBasicMap.get(toolName) != null
                && StringUtils.isNotEmpty(toolMetaBasicMap.get(toolName).getDisplayName())) {
            displayName = toolMetaBasicMap.get(toolName).getDisplayName();
        } else {
            ToolMetaBaseVO toolMetaBaseVO = getToolFromCache(toolName);
            displayName = toolMetaBaseVO.getDisplayName();
        }
        return displayName;
    }

    /**
     * 从缓存中获取所有工具
     *
     * @param isDetail
     * @param isAdmin
     * @return
     */
    @Override
    public Map<String, ToolMetaBaseVO> getToolMetaListFromCache(boolean isDetail, boolean isAdmin) {
        // TODO 查询工具元数据列表
        return null;
    }

    /**
     * 根据工具名从缓存中获取工具完整信息
     *
     * @param toolName
     * @return
     */
    @Override
    public ToolMetaDetailVO getToolDetailFromCache(String toolName) {
        return null;
    }

    @Override
    public List<String> getToolDetailByDimension(String dimension) {
        if (StringUtils.isBlank(dimension)) {
            return null;
        }
        
        // 双重检查，避免并发竞态条件
        Set<ToolMetaBaseVO> dimensionSet = toolMetaBasicDimensionMap.get(dimension);
        if (dimensionSet == null || CollectionUtils.isEmpty(dimensionSet)) {
            synchronized (this) {
                // 再次检查，防止重复加载
                dimensionSet = toolMetaBasicDimensionMap.get(dimension);
                if (dimensionSet == null || CollectionUtils.isEmpty(dimensionSet)) {
                    loadToolBaseCache();
                    dimensionSet = toolMetaBasicDimensionMap.get(dimension);
                }
            }
        }
        
        // 防御性检查：如果加载后仍然为空，返回空列表
        if (dimensionSet == null) {
            log.warn("Tool dimension set is still null after loading cache for dimension: {}", dimension);
            return Lists.newArrayList();
        }
        
        return dimensionSet.stream()
                .map(ToolMetaBaseVO::getName)
                .collect(Collectors.toList());
    }

    @Override
    public List<ToolOption> getToolOptionsByToolName(String toolName) {
        return null;
    }

    @Override
    public String getDisplayNameByLocale(String toolName, Locale locale) {
        try {
            String language = locale.getLanguage();
            Map<String, String> displayNameMap = TOOL_DISPLAY_NAME_MAP.get(language);
            if (MapUtils.isNotEmpty(displayNameMap)) {
                return displayNameMap.get(toolName);
            }

            // client切面已注入language头
            Result<List<ToolMetaBaseVO>> response = client.get(ServiceToolMetaRestResource.class).toolList(false);
            if (response == null || response.isNotOk()) {
                return "";
            }

            List<ToolMetaBaseVO> toolMetaBaseVOList = response.getData();
            if (CollectionUtils.isEmpty(toolMetaBaseVOList)) {
                return "";
            }

            displayNameMap = toolMetaBaseVOList.stream()
                    .collect(Collectors.toMap(ToolMetaBaseVO::getName, ToolMetaBaseVO::getDisplayName, (k1, k2) -> k1));
            TOOL_DISPLAY_NAME_MAP.put(language, displayNameMap);

            return displayNameMap.get(toolName);
        } catch (Throwable t) {
            log.error("getToolDisplayName fail, tool name: {}, local: {}", toolName, locale, t);

            return "";
        }
    }

    /**
     * 根据工具名从缓存中获取工具完整信息
     *
     * @param toolName
     * @return
     */
    private ToolMetaBaseVO getToolFromCache(String toolName) {
        ToolMetaBaseVO toolMetaBaseVOResult = null;
        List<ToolMetaBaseVO> toolMetaBaseVOS = loadToolBaseCache();
        if (CollectionUtils.isNotEmpty(toolMetaBaseVOS)) {
            for (ToolMetaBaseVO toolMetaBaseVO : toolMetaBaseVOS) {
                if (toolName.equals(toolMetaBaseVO.getName())) {
                    toolMetaBaseVOResult = toolMetaBaseVO;
                    break;
                }
            }
        }
        if (Objects.isNull(toolMetaBaseVOResult)) {
            log.error("tool[{}] is invalid.", toolName);
            throw new CodeCCException(CommonMessageCode.INVALID_TOOL_NAME, new String[]{toolName}, null);
        }

        return toolMetaBaseVOResult;
    }
}
