/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.tencent.bk.codecc.defect.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 并发告警提交配置缓存
 * 
 * @date 2021/3/15
 * @version V1.0
 */
@Slf4j
@Component
public class ConcurrentDefectTracingConfigCache {
    /**
     * vip项目最大并发数配置
     */
    final String CLUSTER_VIP_CONCURRENT_LIMIT = "CLUSTER_VIP_CONCURRENT_LIMIT";
    /**
     * 普通项目最大并发数配置
     */
    final String CLUSTER_NORMAL_CONCURRENT_LIMIT = "CLUSTER_NORMAL_CONCURRENT_LIMIT";
    /**
     * vip项目清单配置
     */
    final String CLUSTER_VIP_CONCURRENT_LIST = "CLUSTER_VIP_CONCURRENT_LIST";

    /**
     * 普通项目聚类任务下发延迟
     */
    final String CLUSTER_TASK_SEND_NORMAL_DELAY = "CLUSTER_TASK_SEND_NORMAL_DELAY";

    /**
     * 普通项目聚类任务下发延迟
     */
    final String CLUSTER_TASK_SEND_VIP_DELAY = "CLUSTER_TASK_SEND_VIP_DELAY";
    /**
     * 聚类并发配置
     */
    private final String CLUSTER_CONCURRENT_CONFIG = "CLUSTER_CONCURRENT_CONFIG";

    @Autowired
    private Client client;
    /**
     * 并发分析的配置参数缓存，每2小时刷新一次
     */
    private LoadingCache<String, String> concurrentLimitCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(2, TimeUnit.HOURS)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(@NotNull String key) {
                    try {
                        List<BaseDataVO> concurrentAnalyzeConfigs = getConcurrentLimitConfigs(key);
                        return concurrentAnalyzeConfigs.get(0).getParamValue();
                    } catch (Exception e) {
                        return null;
                    }

                }
            });


    /**
     * 获取vip项目清单
     */
    private LoadingCache<String, Set<Long>> vipTaskListCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(30, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Set<Long>>() {
                @Override
                public Set<Long> load(@NotNull String key) {
                    try {
                        List<BaseDataVO> concurrentAnalyzeConfigs = getConcurrentLimitConfigs(key);
                        return Arrays.stream(concurrentAnalyzeConfigs.get(0).getParamValue().split(";"))
                                .map(Long::valueOf).collect(Collectors.toSet());
                    } catch (Exception e) {
                        return null;
                    }

                }
            });


    /**
     * 获取任务下发延迟
     */
    private LoadingCache<String, String> taskSendDelayCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(20, TimeUnit.MINUTES)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(@NotNull String key) {
                    try {
                        List<BaseDataVO> concurrentAnalyzeConfigs = getConcurrentLimitConfigs(key);
                        String paramValue;
                        if (CollectionUtils.isEmpty(concurrentAnalyzeConfigs)
                                || StringUtils.isBlank(concurrentAnalyzeConfigs.get(0).getParamValue())) {
                            paramValue = "3000";
                        } else {
                            paramValue = concurrentAnalyzeConfigs.get(0).getParamValue();
                        }
                        return paramValue;
                    } catch (Exception e) {
                        return null;
                    }

                }
            });


    /**
     * 并发分析的参数配置的缓存
     *
     * @param paramCode
     * @return
     */
    private List<BaseDataVO> getConcurrentLimitConfigs(String paramCode) {
        Result<List<BaseDataVO>> paramsResult = client.get(ServiceBaseDataResource.class)
                .getInfoByTypeAndCode(CLUSTER_CONCURRENT_CONFIG, paramCode);
        if (paramsResult.isNotOk() || CollectionUtils.isEmpty(paramsResult.getData())) {
            log.error("param list is empty! param type: {}, param code: {}", CLUSTER_CONCURRENT_CONFIG, paramCode);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return paramsResult.getData();
    }

    /**
     * 获取vip任务清单
     * @return
     */
    public Set<Long> getVipTaskSet() {
        return vipTaskListCache.getUnchecked(CLUSTER_VIP_CONCURRENT_LIST);
    }

    /**
     * 获取最大并发限制
     * @param isVip
     * @return
     */
    public Integer getConcurrentLimit(Boolean isVip) {
        if (null != isVip && isVip) {
            return Integer.valueOf(concurrentLimitCache.getUnchecked(CLUSTER_VIP_CONCURRENT_LIMIT));
        } else {
            return Integer.valueOf(concurrentLimitCache.getUnchecked(CLUSTER_NORMAL_CONCURRENT_LIMIT));
        }
    }

    /**
     * 获取任务下发的延迟
     * @param isVip
     * @return
     */
    public Integer getTaskSendDelay(Boolean isVip) {
        if (null != isVip && isVip) {
            return Integer.valueOf(taskSendDelayCache.getUnchecked(CLUSTER_TASK_SEND_VIP_DELAY));
        } else {
            return Integer.valueOf(taskSendDelayCache.getUnchecked(CLUSTER_TASK_SEND_NORMAL_DELAY));
        }
    }


}
