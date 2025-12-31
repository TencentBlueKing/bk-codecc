package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.service.ToolBuildInfoService;
import com.tencent.bk.codecc.defect.service.IIgnoredNegativeDefectService;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO.TaskInfoVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BusinessType;
import com.tencent.devops.common.constant.ComConstants.ToolType;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Lint工具批量恢复忽略实现类
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
@Service("LINTBatchRevertIgnoreBizService")
public class LintBatchRevertIgnoreBizServiceImpl extends AbstractLintBatchDefectProcessBizService {

    @Autowired
    private LintDefectV2Dao defectDao;
    @Autowired
    private IIgnoredNegativeDefectService iIgnoredNegativeDefectService;

    @Autowired
    private ToolBuildInfoService toolBuildInfoService;

    /**
     * 获取批处理类型对应的告警状态条件
     * 忽略告警、告警处理人分配、告警标志修改针对的都是待修复告警，而恢复忽略针对的是已忽略告警
     *
     * @param queryCondObj
     * @return
     */
    @Override
    protected Set<String> getStatusCondition(DefectQueryReqVO queryCondObj) {
        return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.IGNORE.value()));
    }

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        List<LintDefectV2Entity> defects = ((List<LintDefectV2Entity>) defectList).stream()
                .filter(it -> (it.getStatus() & ComConstants.DefectStatus.IGNORE.value()) > 0
                        && (null == it.getIgnoreCommentDefect() || !it.getIgnoreCommentDefect()))
                .map(it -> {
                    it.setStatus(it.getStatus() - ComConstants.DefectStatus.IGNORE.value());
                    return it;
                }).collect(Collectors.toList());

        defectDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defects, 0,
                null, null, true);

        // 将缺陷列表按任务ID分组，并收集每个任务对应的工具名称集合
        List<BatchDefectProcessReqVO.TaskInfoVO> taskInfoVOS = groupDefectsByTaskIdAndToolName(defects);
        if (!CollectionUtils.isEmpty(taskInfoVOS)) {
            if (CollectionUtils.isEmpty(batchDefectProcessReqVO.getTaskInfos())) {
                batchDefectProcessReqVO.setTaskInfos(taskInfoVOS);
            } else {
                batchDefectProcessReqVO.getTaskInfos().addAll(taskInfoVOS);
            }
        }
        // 设置工具全量扫描
        toolBuildInfoService.batchSetForceFullScan(batchDefectProcessReqVO.getTaskInfos());

        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
    }

    /**
     * 将缺陷列表按任务ID分组，并转换为TaskInfoVO列表
     *
     * @param defects 缺陷实体列表
     * @return TaskInfoVO列表，包含任务ID和对应的工具名称集合
     */
    private List<BatchDefectProcessReqVO.TaskInfoVO> groupDefectsByTaskIdAndToolName(List<LintDefectV2Entity> defects) {
        if (CollectionUtils.isEmpty(defects)) {
            return Collections.emptyList();
        }
        return defects.stream()
                        .filter(Objects::nonNull)                  // 过滤List中的null元素
                        .filter(entity -> entity.getToolName() != null)  // 过滤toolName为null的实体
                        .collect(Collectors.groupingBy(
                                LintDefectV2Entity::getTaskId,    // 按任务ID分组, taskId -> long
                                Collectors.mapping(
                                        LintDefectV2Entity::getToolName,  // 提取工具名称
                                        Collectors.toCollection(HashSet::new)
                                )
                        ))
                        .entrySet().stream()
                        .map(entry -> {
                            BatchDefectProcessReqVO.TaskInfoVO taskInfo = new BatchDefectProcessReqVO.TaskInfoVO();
                            taskInfo.setTaskId(entry.getKey());
                            taskInfo.setToolNames(entry.getValue());
                            return taskInfo;
                        })
                        .collect(Collectors.toList());
    }

    @Override
    protected void doBizByPage(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        Set<String> entityIdSet = new HashSet<>();

        List<LintDefectV2Entity> defects = ((List<LintDefectV2Entity>) defectList).stream()
                .filter(it -> (it.getStatus() & ComConstants.DefectStatus.IGNORE.value()) > 0
                        && (null == it.getIgnoreCommentDefect() || !it.getIgnoreCommentDefect()))
                .map(it -> {
                    it.setStatus(it.getStatus() - ComConstants.DefectStatus.IGNORE.value());
                    entityIdSet.add(it.getEntityId());
                    return it;
                }).collect(Collectors.toList());

        defectDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defects, 0,
                null, null, true);

        iIgnoredNegativeDefectService.batchDeleteIgnoredDefects(batchDefectProcessReqVO.getTaskId(), entityIdSet);

        // 将缺陷列表按任务ID分组，并收集每个任务对应的工具名称集合
        List<BatchDefectProcessReqVO.TaskInfoVO> taskInfoVOS = groupDefectsByTaskIdAndToolName(defects);
        if (CollectionUtils.isEmpty(taskInfoVOS)) {
            return;
        }
        if (CollectionUtils.isEmpty(batchDefectProcessReqVO.getTaskInfos())) {
            batchDefectProcessReqVO.setTaskInfos(taskInfoVOS);
            return;
        }
        Map<Long, TaskInfoVO> cache = batchDefectProcessReqVO.getTaskInfos().stream()
                .collect(Collectors.toMap(TaskInfoVO::getTaskId, Function.identity()));
        for (TaskInfoVO taskInfoVO : taskInfoVOS) {
            if (cache.containsKey(taskInfoVO.getTaskId())) {
                cache.get(taskInfoVO.getTaskId()).getToolNames().addAll(taskInfoVO.getToolNames());
            } else {
                batchDefectProcessReqVO.getTaskInfos().add(taskInfoVO);
            }
        }
    }

    @Override
    protected void processAfterAllPageDone(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        // 设置工具全量扫描
        toolBuildInfoService.batchSetForceFullScan(batchDefectProcessReqVO.getTaskInfos());
        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
    }

    @Override
    protected Pair<BusinessType, ToolType> getBusinessTypeToolTypePair() {
        return Pair.of(BusinessType.REVERT_IGNORE, ToolType.STANDARD);
    }
}
