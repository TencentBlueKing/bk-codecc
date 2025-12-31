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

package com.tencent.bk.codecc.defect.utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.codecc.defect.api.ServiceReportTaskLogRestResource;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.ServiceToolConfigRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.RiskFactor;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 外部服务公共调度器
 *
 * @version V1.0
 * @date 2019/6/5
 */
@Component
@Slf4j
public class ThirdPartySystemCaller {

    @Autowired
    private Client client;

    private LoadingCache<RiskFactor, Entry<Integer, Integer>> ccnRiskFactorCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES).build(
                    new CacheLoader<RiskFactor, Entry<Integer, Integer>>() {
                        @Override
                        public Map.Entry<Integer, Integer> load(@NotNull ComConstants.RiskFactor riskFactor) {
                            Map<String, String> riskFactorMap = getRiskFactorConfig(ComConstants.Tool.CCN.name());

                            int sh = Integer.parseInt(riskFactorMap.get(ComConstants.RiskFactor.SH.name()));
                            int h = Integer.parseInt(riskFactorMap.get(ComConstants.RiskFactor.H.name()));
                            int m = Integer.parseInt(riskFactorMap.get(ComConstants.RiskFactor.M.name()));

                            switch (riskFactor) {
                                case SH:
                                    return new AbstractMap.SimpleEntry<>(sh, null);
                                case H:
                                    return new AbstractMap.SimpleEntry<>(h, sh);
                                case M:
                                    return new AbstractMap.SimpleEntry<>(m, h);
                                case L:
                                    return new AbstractMap.SimpleEntry<>(null, m);
                                default:
                                    return new AbstractMap.SimpleEntry<>(null, null);
                            }
                        }
                    }
            );

    /**
     * 调用task模块的接口获取任务信息
     *
     * @param streamName
     * @return
     */
    @NotNull
    public TaskDetailVO getTaskInfo(String streamName) {
        Result<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfo(streamName);
        if (taskInfoResult.isNotOk() || null == taskInfoResult.getData()) {
            log.error("get task info fail! stream name is: {}, msg: {}", streamName, taskInfoResult.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return taskInfoResult.getData();
    }

    /**
     * 调用task模块的接口获取任务信息
     *
     * @param taskId
     * @return
     */
    @NotNull
    public TaskDetailVO getTaskInfoWithoutToolsByTaskId(Long taskId) {
        Result<TaskDetailVO> taskDetailResult = client.get(ServiceTaskRestResource.class)
                .getTaskInfoWithoutToolsByTaskId(taskId);
        if (taskDetailResult.isNotOk() || null == taskDetailResult.getData()) {
            log.error("get task info fail! taskId: {}, msg: {}", taskId, taskDetailResult.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return taskDetailResult.getData();
    }

    public TaskDetailVO getNullableTaskInfoWithoutToolsByTaskId(Long taskId) {
        Result<TaskDetailVO> taskDetailResult = client.get(ServiceTaskRestResource.class)
                .getTaskInfoWithoutToolsByTaskId(taskId);
        if (taskDetailResult.isNotOk() || null == taskDetailResult.getData()) {
            return null;
        }
        return taskDetailResult.getData();
    }

    public TaskDetailVO geTaskInfoTaskId(Long taskId) {
        try {
            Result<TaskDetailVO> response =
                    client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);

            if (response.isNotOk() || response.getData() == null) {
                log.error("fail to get task info: {}", taskId);
                return null;
            }
            return response.getData();
        } catch (Throwable e) {
            log.info("fail to get task info error, taskId : {}", taskId, e);
        }
        return null;
    }

    public List<TaskDetailVO> geTaskInfoTaskIds(Collection<Long> taskIds) {
        try {
            QueryTaskListReqVO reqVO = new QueryTaskListReqVO();
            reqVO.setTaskIds(taskIds);
            Result<List<TaskDetailVO>> response =
                    client.get(ServiceTaskRestResource.class).batchGetTaskList(reqVO);
            if (response.isNotOk() || response.getData() == null) {
                log.error("fail to get task info: {}", taskIds);
                return Collections.emptyList();
            }
            return response.getData();
        } catch (Throwable e) {
            log.info("fail to get task info error, taskId : {}", taskIds, e);
        }
        return Collections.emptyList();
    }

    /**
     * 获取重复率的风险系数基本数据
     *
     * @return
     */
    @NotNull
    public Map<String, String> getRiskFactorConfig(String toolName) {
        //获取风险系数值
        Result<List<BaseDataVO>> baseDataResult = client.get(ServiceBaseDataResource.class)
                .getInfoByTypeAndCode(ComConstants.PREFIX_RISK_FACTOR_CONFIG, toolName);

        if (baseDataResult.isNotOk() || null == baseDataResult.getData()) {
            log.error("get risk coefficient fail!");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        return baseDataResult.getData().stream()
                .collect(Collectors.toMap(BaseDataVO::getParamName, BaseDataVO::getParamValue, (k, v) -> v));
    }

    public void uploadTaskLog(UploadTaskLogStepVO uploadTaskLogStepVO) {
        Result result = client.get(ServiceReportTaskLogRestResource.class).uploadTaskLog(uploadTaskLogStepVO);

        if (result.isNotOk()) {
            log.error("upload TaskLog fail! message: {} {}", uploadTaskLogStepVO.getStreamName(), result.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
    }

    public List<BaseDataVO> getParamsByType(String paramType) {
        Result<List<BaseDataVO>> result = client.get(ServiceBaseDataResource.class).getParamsByType(paramType);

        if (result.isNotOk() || null == result.getData()) {
            log.error("get param by type fail! message: {} {}", paramType, result.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        return result.getData();
    }

    /**
     * 根据任务英文名查询任务信息，不包含工具信息
     *
     * @param streamName 流名称
     * @return vo
     */
    @NotNull
    public TaskDetailVO getTaskInfoWithoutToolsByStreamName(String streamName) {
        Result<TaskDetailVO> taskInfoResult =
                client.get(ServiceTaskRestResource.class).getTaskInfoWithoutToolsByStreamName(streamName);
        if (taskInfoResult.isNotOk() || null == taskInfoResult.getData()) {
            log.error("get task info fail! stream name is: {}, msg: {}", streamName, taskInfoResult.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return taskInfoResult.getData();
    }

    /**
     * 根据任务id批量获取project id
     */
    public Map<Long, String> getTaskProjectIdMap(Set<Long> taskIdSet) {
        QueryTaskListReqVO reqVO = new QueryTaskListReqVO();
        reqVO.setTaskIds(taskIdSet);
        Result<Map<Long, String>> result = client.get(ServiceTaskRestResource.class).getProjectIdMapByTaskId(reqVO);
        if (result.isNotOk() || result.getData() == null) {
            log.error("getTaskProjectIdMap fail!");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return result.getData();
    }

    /**
     * 获取指定工具已下架的任务id
     *
     * @param toolSet 工具名集合
     * @return map
     */
    public Map<String, Set<Long>> getToolTaskIdMap(Set<String> toolSet) {
        Result<Map<String, Set<Long>>> result =
                client.get(ServiceTaskRestResource.class).queryTaskIdByWithdrawTool(toolSet);
        if (result.isNotOk() || result.getData() == null) {
            log.error("getToolTaskIdMap fail! toolSet: {}", toolSet);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return result.getData();
    }


    /**
     * 获取任务配置的工具类别（包含已下架）
     *
     * @param taskId
     * @return
     */
    public Set<String> getTaskConfigTools(Long taskId) {
        if (taskId == null) {
            return Collections.emptySet();
        }
        // 获取任务所有使用过的工具
        Result<List<ToolConfigInfoVO>> result = client.get(ServiceToolConfigRestResource.class).getByTaskId(taskId);
        if (result.isNotOk() || CollectionUtils.isEmpty(result.getData())) {
            log.error("handler task invalid tools {} result not ok or empty", taskId);
            return Collections.emptySet();
        }
        return result.getData().stream().map(ToolConfigInfoVO::getToolName).filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    public Map.Entry<Integer, Integer> getCCNRiskFactorConfig(ComConstants.RiskFactor riskFactor) {
        //获取风险系数值
        return ccnRiskFactorCache.getUnchecked(riskFactor);
    }

    public List<Long> getTaskIdsByProjectId(String projectId) {
        if (StringUtils.isBlank(projectId)) {
            return Collections.emptyList();
        }
        // 获取任务所有使用过的工具
        Result<List<Long>> result = client.get(ServiceTaskRestResource.class).getTaskIdsByProjectId(projectId);
        if (result.isNotOk() || CollectionUtils.isEmpty(result.getData())) {
            log.error("handler project  {} result not ok or empty", projectId);
            return Collections.emptyList();
        }
        return result.getData();
    }
}
