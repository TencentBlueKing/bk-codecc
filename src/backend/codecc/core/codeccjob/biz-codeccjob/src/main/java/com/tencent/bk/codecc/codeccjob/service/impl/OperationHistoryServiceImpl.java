/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.codecc.codeccjob.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.OperationHistoryRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.DefectDao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.codeccjob.service.OperationHistoryService;
import com.tencent.bk.codecc.defect.api.ServiceDefectRestResource;
import com.tencent.bk.codecc.defect.api.ServiceReportDefectRestResource;
import com.tencent.bk.codecc.defect.api.UserIgnoreTypeRestResource;
import com.tencent.bk.codecc.defect.model.OperationHistoryEntity;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.vo.ToolDefectPageVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeProjectConfigVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.constant.CommonMessageCode;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.web.aop.model.OperationHistoryDTO;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 操作记录服务实现类
 *
 * @version V1.0
 * @date 2019/6/17
 */
@Slf4j
@Service
public class OperationHistoryServiceImpl implements OperationHistoryService {

    /**
     * 最大展示告警id数
     */
    private static final int MAX_SHOW_DEFECT_ID_COUNT = 50;
    @Autowired
    private OperationHistoryRepository operationHistoryRepository;
    @Autowired
    private DefectDao defectDao;
    @Autowired
    private CCNDefectDao ccnDefectDao;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private ToolMetaCacheService toolMetaCacheService;
    @Autowired
    private Client client;

    @Override
    public void saveOperationHistory(OperationHistoryDTO operationHistoryDTO) {
        log.info("saveOperationHistory: {}", operationHistoryDTO);
        Long taskId = operationHistoryDTO.getTaskId();
        if (taskId == 0 && StringUtils.isNotBlank(operationHistoryDTO.getPipelineId())) {
            taskId = Objects.requireNonNull(
                            client.get(ServiceTaskRestResource.class).getPipelineTask(
                                            operationHistoryDTO.getPipelineId(),
                                            operationHistoryDTO.getMultiPipelineMark(),
                                            operationHistoryDTO.getOperator())
                                    .getData())
                    .getTaskId();
        }

        OperationHistoryEntity operationHistoryEntity = new OperationHistoryEntity();
        operationHistoryEntity.setTaskId(taskId);
        operationHistoryEntity.setFuncId(operationHistoryDTO.getFuncId());
        operationHistoryEntity.setOperType(operationHistoryDTO.getOperType());
        operationHistoryEntity.setTime(operationHistoryDTO.getTime());
        operationHistoryEntity.setToolName(operationHistoryDTO.getToolName());
        // 自定义格式化操作记录描述
        operationHistoryEntity.setParamArray(convertParamInfo(operationHistoryDTO, operationHistoryEntity));
        operationHistoryEntity.setOperator(operationHistoryDTO.getOperator());

        long currentTime = System.currentTimeMillis();
        operationHistoryEntity.setCreatedDate(currentTime);
        operationHistoryEntity.setCreatedBy(ComConstants.SYSTEM_USER);
        operationHistoryEntity.setUpdatedDate(currentTime);
        operationHistoryEntity.setUpdatedBy(ComConstants.SYSTEM_USER);
        //保存操作记录信息
        operationHistoryRepository.save(operationHistoryEntity);
    }

    /**
     * 自定义组装(转换)操作记录描述
     *
     * @param operaHisDTO dto
     * @param operationHistoryEntity entity
     * @return array
     */
    private String[] convertParamInfo(OperationHistoryDTO operaHisDTO, OperationHistoryEntity operationHistoryEntity) {
        if (operaHisDTO == null || StringUtils.isBlank(operaHisDTO.getOperType())) {
            log.warn("any param is empty!");
            return new String[]{};
        }

        String[] paramArrResult;
        long taskId = operaHisDTO.getTaskId();
        String operaType = operaHisDTO.getOperType();
        String[] paramArray = operaHisDTO.getParamArray() == null ? new String[]{} : operaHisDTO.getParamArray();
        boolean isDataMigrationSuccessful = false;
        try {
            if (StringUtils.isNotEmpty(operaHisDTO.getOperMsg())) {
                ObjectNode jsonNode = JsonUtil.INSTANCE.getObjectMapper()
                        .readValue(operaHisDTO.getOperMsg(), ObjectNode.class);
                String jsonFieldName = "dataMigrationSuccessful";

                if (jsonNode.has(jsonFieldName)) {
                    isDataMigrationSuccessful = jsonNode.get(jsonFieldName).booleanValue();
                }
            }
            // NOCC:EmptyCatchBlock(设计如此:)
        } catch (JsonProcessingException ignore) {

        }

        log.info("op history: {}, {}, {}", taskId, operaType, isDataMigrationSuccessful);

        // 操作记录相关信息需要转换为更详细内容的类型
        if (ComConstants.DEFECT_IGNORE.equals(operaType) || ComConstants.REVERT_IGNORE.equals(operaType)
                || ComConstants.DEFECT_MARKED.equals(operaType) || ComConstants.DEFECT_UNMARKED.equals(operaType)
                || ComConstants.ASSIGN_DEFECT.equals(operaType) || ComConstants.ISSUE_DEFECT.equals(operaType)) {
            if (paramArray.length == 0) {
                log.warn("operation type {} has no param array! task id: {}", operaType, taskId);
                return paramArray;
            }
            // 适配某些工具不传维度
            String dimension = operaHisDTO.getDimension();
            if (StringUtils.isBlank(dimension)) {
                if (StringUtils.isNotEmpty(operaHisDTO.getToolName())) {
                    dimension = toolMetaCacheService.getToolBaseMetaCache(operaHisDTO.getToolName()).getType();
                } else {
                    log.warn("toolName or dimension is empty!!");
                    return paramArray;
                }
            }

            Set<String> defectIdSet = Sets.newHashSet(paramArray[0].split(ComConstants.STRING_SPLIT));

            String defectIdInfo;
            if (ComConstants.CommonJudge.COMMON_Y.value().equals(operaHisDTO.getOperTypeName())) {
                // 1.全选/按文件维度
                String queryDefectCondition = operaHisDTO.getOperMsg();
                log.info("queryDefectCondition: {}", queryDefectCondition);
                if (StringUtils.isEmpty(queryDefectCondition)) {
                    log.error("select all operation don't have queryDefectCondition, abort operation history!");
                    return paramArray;
                }
                DefectQueryReqVO queryCondObj = JsonUtil.INSTANCE.to(queryDefectCondition, DefectQueryReqVO.class);

                // 告警操作，只对待修复告警操作，忽略操作是由于平台已处理 再去查询需要带上已忽略
                Set<String> statusSet = Sets.newHashSet(String.valueOf(DefectStatus.NEW.value()),
                            String.valueOf(DefectStatus.NEW.value() | DefectStatus.IGNORE.value()));

                queryCondObj.setStatus(statusSet);

                log.info("query start~");
                // 分开查询 先聚合查询所有工具，再按照工具查询50个ID与总数
                List<String> toolList = getToolListByDefectQuery(taskId, queryCondObj);
                if (CollectionUtils.isEmpty(toolList)) {
                    log.error("taskId:{} toolList is empty! abort.", taskId);
                    return paramArray;
                }
                queryCondObj.setProjectId(operaHisDTO.getProjectId());
                queryCondObj.setUserId(operaHisDTO.getOperator());
                // 逐个查询，仅查询50个与总数, 调用查询变多，但是大小可控
                int pageNum = 0;
                List<ToolDefectPageVO> hasDefectTools = new LinkedList<>();
                for (String tool : toolList) {
                    DefectQueryReqVO toolReqVO = new DefectQueryReqVO();
                    BeanUtils.copyProperties(queryCondObj, toolReqVO);
                    if (CollectionUtils.isEmpty(queryCondObj.getTaskIdList())) {
                        toolReqVO.setTaskIdList(Collections.singletonList(operaHisDTO.getTaskId()));
                    }
                    // 设置单个工具查询
                    toolReqVO.setToolNameList(Collections.singletonList(tool));
                    ToolDefectPageVO toolDefectIdList = client.get(ServiceDefectRestResource.class)
                            .queryDefectIdPageByCondition(taskId, toolReqVO, pageNum,
                                    MAX_SHOW_DEFECT_ID_COUNT).getData();
                    if (toolDefectIdList != null && toolDefectIdList.getCount() != null
                            && toolDefectIdList.getCount() > 0) {
                        toolDefectIdList.setToolName(tool);
                        hasDefectTools.add(toolDefectIdList);
                    }
                }
                log.info("query finish!");
                log.info("hasDefectTools {}", hasDefectTools);
                if (CollectionUtils.isEmpty(hasDefectTools)) {
                    log.error("taskId:{} hasDefectTools is empty! abort.", taskId);
                    return paramArray;
                }
                operationHistoryEntity.setToolName(hasDefectTools.stream().map(ToolDefectPageVO::getToolName)
                        .collect(Collectors.joining(ComConstants.STRING_SPLIT)));
                defectIdInfo = getOperaDefectIdInfo(hasDefectTools);
            } else {
                // 2.按告警id
                // 获取工具对应的告警id映射
                Map<String, List<String>> toolDefectIdMap = getToolDefectIdMap(taskId, dimension, defectIdSet,
                        isDataMigrationSuccessful);
                this.setToolNameByOperaInfo(toolDefectIdMap, operationHistoryEntity);
                // 组装操作告警的信息
                defectIdInfo = getOperaDefectIdInfo(toolDefectIdMap);
            }

            // 忽略告警有忽略理由
            if (ComConstants.DEFECT_IGNORE.equals(operaType)) {
                // 忽略告警分开传值 paramArray[1] 传递 ignoreReasonType  paramArray[2] 传递 ignoreReason
                String ignoreReasonFull = "未忽略";
                if (StringUtils.isNotBlank(paramArray[1])) {
                    Result<IgnoreTypeProjectConfigVO> result = client.get(UserIgnoreTypeRestResource.class).detail(
                            operaHisDTO.getProjectId(), operaHisDTO.getOperator(), Integer.parseInt(paramArray[1]));
                    // 判断是data是否为空，如果为空就返回未忽略
                    if (result.isOk() && result.getData() != null) {
                        String ignoreReasonType = result.getData().getName();
                        if (StringUtils.isNotBlank(paramArray[2])) {
                            ignoreReasonFull = ignoreReasonType + "-" + paramArray[2];
                        } else {
                            ignoreReasonFull = ignoreReasonType;
                        }
                    }
                }
                paramArrResult = new String[]{defectIdInfo, ignoreReasonFull};
            } else if (ComConstants.ASSIGN_DEFECT.equals(operaType)) {
                // 操作告警, 原处理人, 目标处理人
                paramArrResult = new String[]{defectIdInfo, paramArray[1], paramArray[2]};
            } else {
                // 恢复忽略、标记、提单等只需要工具和ID
                paramArrResult = new String[]{defectIdInfo};
            }

            // 代码评论操作记录
        } else if (ComConstants.CODE_COMMENT_ADD.equals(operaType) || ComConstants.CODE_COMMENT_DEL.equals(operaType)) {
            if (paramArray.length == 0 || StringUtils.isBlank(operaHisDTO.getToolName())) {
                log.warn("opera [{}] paramArray or tool is empty!", operaType);
                return new String[]{};
            }

            ToolMetaBaseVO toolMetaBaseVO = toolMetaCacheService.getToolBaseMetaCache(operaHisDTO.getToolName());
            String dimension = toolMetaBaseVO.getType();

            Map<String, List<String>> toolDefectIdMap =
                    getToolDefectIdMap(taskId, dimension, Sets.newHashSet(paramArray[0]), isDataMigrationSuccessful);

            this.setToolNameByOperaInfo(toolDefectIdMap, operationHistoryEntity);

            // 组装操作告警的信息
            String defectIdInfo = getOperaDefectIdInfo(toolDefectIdMap);
            paramArrResult = new String[]{defectIdInfo, paramArray[1]};
        } else {
            paramArrResult = paramArray;
        }

        return paramArrResult;
    }

    private List<String> getToolListByDefectQuery(long taskId, DefectQueryReqVO queryCondObj) {
        if (CollectionUtils.isNotEmpty(queryCondObj.getToolNameList())) {
            // 制定了工具，直接返回
            return queryCondObj.getToolNameList();
        } else {
            // 先获取现在的工具配置
            TaskDetailVO taskDetailVO = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId).getData();
            if (taskDetailVO == null || CollectionUtils.isEmpty(taskDetailVO.getToolConfigInfoList())) {
                return Collections.emptyList();
            }
            List<String> taskToolList = taskDetailVO.getToolConfigInfoList().stream().map(ToolConfigInfoVO::getToolName)
                    .collect(Collectors.toList());
            // 如果没有配置工具类型，就返回全部，如果设置了，就进行过滤
            List<String> dimensions = null;
            if (CollectionUtils.isNotEmpty(queryCondObj.getDimensionList())) {
                dimensions = queryCondObj.getDimensionList();
            }
            List<String> toolVOs = taskToolList;
            if (CollectionUtils.isNotEmpty(dimensions)) {
                toolVOs = new LinkedList<>();
                for (String tool : taskToolList) {
                    // 过滤掉不在类型范围中的工具
                    ToolMetaBaseVO toolMetaBaseVO = toolMetaCacheService.getToolBaseMetaCache(tool);
                    if (toolMetaBaseVO != null && StringUtils.isNotEmpty(toolMetaBaseVO.getType())
                            && dimensions.contains(toolMetaBaseVO.getType())) {
                        toolVOs.add(tool);
                    }
                }
            }
            return toolVOs;
        }
    }

    /**
     * 组装告警id及限制最长id数
     *
     * @param toolDefectIdMap 工具->id映射
     * @return string
     */
    private String getOperaDefectIdInfo(Map<String, List<String>> toolDefectIdMap) {
        if (toolDefectIdMap == null || toolDefectIdMap.isEmpty()) {
            log.warn("toolDefectIdMap is null or empty!");
            return "--";
        }
        boolean isBatch = false;
        log.info("toolDefectIdMap {}", toolDefectIdMap);
        StringBuilder strBuilder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : toolDefectIdMap.entrySet()) {
            List<String> defectIdList = entry.getValue();
            // 升序排列告警id
            defectIdList.sort(Comparator.comparing(Integer::parseInt));
            int defectIdListLength = defectIdList.size();

            // 超过1个才统计批量个数
            if (defectIdListLength > 1) {
                isBatch = true;
            }

            boolean isOverLength = false;
            if (defectIdListLength >= MAX_SHOW_DEFECT_ID_COUNT) {
                defectIdList = defectIdList.subList(0, MAX_SHOW_DEFECT_ID_COUNT);
                isOverLength = true;
            }

            String idStr = String.join("、", defectIdList);
            String toolDisplayName = toolMetaCacheService.getToolDisplayName(entry.getKey());
            strBuilder.append("【").append(toolDisplayName).append(" ID】").append(idStr);
            // 记录批量操作的统计
            if (isBatch) {
                if (isOverLength) {
                    strBuilder.append("等共");
                } else {
                    strBuilder.append("共");
                }
                strBuilder.append(defectIdListLength).append("个");
            }
            strBuilder.append("，");
        }
        if (strBuilder.length() > 0) {
            strBuilder.deleteCharAt(strBuilder.length() - 1);
        }
        return strBuilder.toString();
    }

    /**
     * 组装告警id
     *
     * @param toolDefectPageVOS
     * @return
     */
    private String getOperaDefectIdInfo(List<ToolDefectPageVO> toolDefectPageVOS) {
        if (toolDefectPageVOS == null || toolDefectPageVOS.isEmpty()) {
            log.warn("toolDefectIdMap is null or empty!");
            return "--";
        }
        StringBuilder strBuilder = new StringBuilder();
        for (ToolDefectPageVO toolDefectPageVO : toolDefectPageVOS) {
            List<String> defectIdList = toolDefectPageVO.getId();
            // 升序排列告警id
            defectIdList.sort(Comparator.comparing(Integer::parseInt));
            String idStr = String.join("、", defectIdList);
            String toolDisplayName = toolMetaCacheService.getToolDisplayName(toolDefectPageVO.getToolName());
            strBuilder.append("【").append(toolDisplayName).append(" ID】").append(idStr);
            if (defectIdList.size() < toolDefectPageVO.getCount()) {
                strBuilder.append("等共");
            } else {
                strBuilder.append("共");
            }
            strBuilder.append(toolDefectPageVO.getCount()).append("个");
            strBuilder.append("，");
        }
        if (strBuilder.length() > 0) {
            strBuilder.deleteCharAt(strBuilder.length() - 1);
        }
        return strBuilder.toString();
    }

    /**
     * 获取任务指定告警实体ID
     *
     * @param taskId 任务ID
     * @param dimension 维度
     * @param defectIdSet 告警实体ID集合
     * @return map tool,id
     */
    private Map<String, List<String>> getToolDefectIdMap(
            long taskId, String dimension,
            Set<String> defectIdSet, boolean isDataMigrationSuccessful
    ) {
        Map<String, List<String>> toolDefectIdMap;
        // 兼容存在多dimension,eg: "SECURITY,STANDARD"
        List<String> dimensionList = Arrays.asList(dimension.split(ComConstants.STRING_SPLIT));
        log.info("split : {}", dimensionList);
        if (isDataMigrationSuccessful
                || dimensionList.contains(ComConstants.ToolType.STANDARD.name())
                || dimensionList.contains(ComConstants.ToolType.SECURITY.name())) {
            List<LintDefectV2Entity> lintDefectV2Entities =
                    lintDefectV2Dao.findByTaskAndEntityIdSet(taskId, defectIdSet);
            toolDefectIdMap = lintDefectV2Entities.stream().collect(Collectors
                    .groupingBy(LintDefectV2Entity::getToolName,
                            Collectors.mapping(LintDefectV2Entity::getId, Collectors.toList())));

        } else if (dimensionList.contains(ComConstants.ToolType.CCN.name())) {
            List<CCNDefectEntity> ccnDefectEntities = ccnDefectDao.findByTaskAndEntityIdSet(taskId, defectIdSet);
            List<String> idList =
                    ccnDefectEntities.stream().map(CCNDefectEntity::getId).collect(Collectors.toList());
            toolDefectIdMap = Maps.newHashMap();
            toolDefectIdMap.put(ComConstants.Tool.CCN.name(), idList);
        } else if (dimensionList.contains(ComConstants.ToolType.DEFECT.name())) {
            List<LintDefectV2Entity> lintDefectV2Entities =
                    lintDefectV2Dao.findByTaskAndEntityIdSet(taskId, defectIdSet);
            toolDefectIdMap = lintDefectV2Entities.stream().collect(Collectors
                    .groupingBy(LintDefectV2Entity::getToolName,
                            Collectors.mapping(LintDefectV2Entity::getId, Collectors.toList())));
        } else {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                    "Not implemented tool dimension!" + dimension);
        }
        return toolDefectIdMap;
    }

    /**
     * 按文件维度忽略告警，查询包含的告警id
     *
     * @param taskId 任务id
     * @param dimension 工具维度
     * @param fileSet 文件路径集合
     * @return map
     */
    private Map<String, List<String>> getToolDefectIdMapByFileSet(long taskId, String dimension, Set<String> fileSet) {
        if (CollectionUtils.isEmpty(fileSet)) {
            log.warn("getToolDefectIdMapByFileSet file list is empty! taskId: {}", taskId);
            return Maps.newHashMap();
        }
        List<String> toolSet = toolMetaCacheService.getToolDetailByDimension(dimension);
        if (CollectionUtils.isEmpty(toolSet)) {
            log.warn("getToolDetailByDimension is empty! taskId: {}, dimension: {}", taskId, dimension);
            return Maps.newHashMap();
        }

        List<LintDefectV2Entity> lintDefectV2Entities = lintDefectV2Dao
                .findDefectsByFilePath(taskId, Sets.newHashSet(toolSet), ComConstants.DefectStatus.NEW.value(),
                        fileSet);

        return lintDefectV2Entities.stream().collect(Collectors.groupingBy(LintDefectV2Entity::getToolName,
                Collectors.mapping(LintDefectV2Entity::getId, Collectors.toList())));
    }

    /**
     * 为方便统计相关操作涉及的工具
     *
     * @param toolDefectIdMap tool, defect list
     * @param entity entity
     */
    private void setToolNameByOperaInfo(Map<String, List<String>> toolDefectIdMap, OperationHistoryEntity entity) {
        if (toolDefectIdMap != null && !toolDefectIdMap.isEmpty()) {
            entity.setToolName(String.join(ComConstants.STRING_SPLIT, toolDefectIdMap.keySet()));
        }
    }
}
