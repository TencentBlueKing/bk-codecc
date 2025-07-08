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

package com.tencent.devops.common.service;

import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BizServiceFlag;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;

/**
 * 多工具业务处理器的工厂类
 *
 * @version V2.6
 * @date 2018/1/18
 */
@Slf4j
public class BizServiceFactory<T> {

    /**
     * 为不同类型的工具创建相应的数据报表处理器
     *
     * @param toolName
     * @param flag 用于区分不同的处理器
     * @return
     */
    public T doCreateBizService(String toolName, BizServiceFlag flag, String businessType, Class<T> clz) {
        String toolProcessorBeanName = String.format("%s%s%s%s", toolName, flag.getFlag(),
                businessType, ComConstants.BIZ_SERVICE_POSTFIX);
        String patternProcessorBeanName = null;
        String commonProcessorBeanName = null;

        // 获取工具名称开头的处理类
        T processor = getProcessor(clz, toolProcessorBeanName);

        // 获取工具类型开头的处理类
        if (processor == null) {
            ToolMetaCacheService toolMetaCacheService = SpringContextUtil.Companion.getBean(ToolMetaCacheService.class);
            // BizService的bean名（PatternBizTypeBizService）的后缀名,比如：COVERITYBatchMarkDefectBizService
            patternProcessorBeanName = String.format("%s%s%s%s", toolMetaCacheService.getToolPattern(toolName),
                    flag.getFlag(), businessType, ComConstants.BIZ_SERVICE_POSTFIX);
            processor = getProcessor(clz, patternProcessorBeanName);
        }

        // 如果没找到工具的具体处理类，则采用通用的处理器
        if (processor == null) {
            commonProcessorBeanName = String.format("%s%s%s%s", ComConstants.COMMON_BIZ_SERVICE_PREFIX, flag.getFlag(),
                    businessType, ComConstants.BIZ_SERVICE_POSTFIX);
            processor = getProcessor(clz, commonProcessorBeanName);
        }

        if (processor == null) {
            log.error("get bean name [{}, {}, {}] fail!", toolProcessorBeanName, patternProcessorBeanName,
                    commonProcessorBeanName);
            throw new CodeCCException(CommonMessageCode.NOT_FOUND_PROCESSOR);
        }

        return processor;
    }

    /**
     * 为不同类型的工具创建相应的数据报表处理器
     *
     * @param toolName
     * @return
     */
    public T createBizService(String toolName, String businessType, Class<T> clz) {
        return doCreateBizService(toolName, BizServiceFlag.CORE, businessType, clz);
    }

    public T createBizService(String toolName, BizServiceFlag flag, String businessType, Class<T> clz) {
        return doCreateBizService(toolName, flag, businessType, clz);
    }

    /**
     * 为不同类型的工具创建相应的数据报表处理器
     *
     * @param toolName
     * @return
     */
    public T createBizService(String toolName, String dimension, String businessType, Class<T> clz) {
        return createBizService(toolName, dimension, BizServiceFlag.CORE, businessType, clz);
    }

    public T createBizService(String toolName, String dimension, BizServiceFlag flag, String businessType,
            Class<T> clz) {
        if (StringUtils.isNotBlank(toolName)) {
            return createBizService(toolName, flag, businessType, clz);
        }

        if (StringUtils.isNotBlank(dimension)) {
            ToolMetaCacheService toolMetaCacheService = SpringContextUtil.Companion.getBean(ToolMetaCacheService.class);
            List<String> toolNameSet = toolMetaCacheService.getToolDetailByDimension(dimension);
            if (CollectionUtils.isNotEmpty(toolNameSet)) {
                List<String> toolNameList = new ArrayList<>(toolNameSet);
                return createBizService(toolNameList.get(0), flag, businessType, clz);
            }
        }

        throw new CodeCCException(CommonMessageCode.NOT_FOUND_PROCESSOR);
    }

    /**
     * 为不同工具/维度构造业务逻辑器
     *
     * @param toolNameList
     * @param dimensionList
     * @param businessType
     * @param clz
     * @return
     */
    public T createBizService(
            List<String> toolNameList,
            List<String> dimensionList,
            String businessType,
            Class<T> clz
    ) {
        return createBizService(toolNameList, dimensionList, BizServiceFlag.CORE, businessType, clz);
    }

    /**
     * 为不同工具/维度构造业务逻辑器
     *
     * @param toolNameList
     * @param dimensionList
     * @param flag
     * @param businessType
     * @param clz
     * @return
     */
    public T createBizService(
            List<String> toolNameList,
            List<String> dimensionList,
            BizServiceFlag flag,
            String businessType,
            Class<T> clz
    ) {
        // 全不选
        if (CollectionUtils.isEmpty(toolNameList) && CollectionUtils.isEmpty(dimensionList)) {
            return createBizService(ToolPattern.LINT.name(), flag, businessType, clz);
        }

        // 维度多选
        if ((CollectionUtils.isNotEmpty(dimensionList) && dimensionList.size() > 1)) {
            return createBizService(ToolPattern.LINT.name(), flag, businessType, clz);
        }

        // 优先按工具处理，工具可能有多个
        if (CollectionUtils.isNotEmpty(toolNameList)) {
            String toolName = toolNameList.get(0);
            if (ToolType.DUPC_CCN_SCC_CLOC_SET.contains(toolName)) {
                return createBizService(toolName, flag, businessType, clz);
            } else {
                // 根据工具维度抉择是SCA还是LINT （都支持多工具）
                ToolMetaCacheService toolMetaCache = SpringContextUtil.Companion.getBean(ToolMetaCacheService.class);
                String pattern = toolMetaCache.getToolPattern(toolName);
                if (StringUtils.isNotBlank(pattern) && ToolPattern.SCA.name().equals(pattern)) {
                    return createBizService(ToolPattern.SCA.name(), flag, businessType, clz);
                } else {
                    return createBizService(ToolPattern.LINT.name(), flag, businessType, clz);
                }
            }
        }

        // 其次按维度处理, 只有单个
        if (CollectionUtils.isNotEmpty(dimensionList)) {
            String dimension = dimensionList.get(0);
            if (ToolType.DIMENSION_FOR_LINT_PATTERN_SET.contains(dimension)) {
                return createBizService(ToolPattern.LINT.name(), flag, businessType, clz);
            } else {
                return createBizService("", dimension, flag, businessType, clz);
            }
        }

        throw new CodeCCException(CommonMessageCode.NOT_FOUND_PROCESSOR);
    }

    /**
     * 为不同类型的业务创建相应的处理器
     *
     * @param businessType
     * @param clz
     * @return
     */
    public T createBizService(String businessType, Class<T> clz) {
        String beanName = String.format("%s%s", businessType, ComConstants.BIZ_SERVICE_POSTFIX);
        T processor = null;
        try {
            processor = SpringContextUtil.Companion.getBean(clz, beanName);
        } catch (BeansException e) {
            log.error("Bean Name [{}] Not Found:", beanName);
        }

        if (processor == null) {
            log.error("get bean name [{}] fail!", beanName);
            throw new CodeCCException(CommonMessageCode.NOT_FOUND_PROCESSOR);
        }

        return processor;
    }

    private <T> T getProcessor(Class<T> clz, String beanName) {
        T processor = null;
        try {
            processor = SpringContextUtil.Companion.getBean(clz, beanName);
        } catch (BeansException e) {
            //log.error("Bean Name [{}] Not Found:", beanName);
        }
        return processor;
    }
}
