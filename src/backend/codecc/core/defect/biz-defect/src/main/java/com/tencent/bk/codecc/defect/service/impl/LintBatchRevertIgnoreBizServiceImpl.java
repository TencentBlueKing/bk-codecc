package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.IgnoredNegativeDefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BusinessType;
import com.tencent.devops.common.constant.ComConstants.ToolType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

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
    private IgnoredNegativeDefectDao ignoredNegativeDefectDao;

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

        defectDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defects, 0, null, null);

        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
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

        defectDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defects, 0, null, null);

        ignoredNegativeDefectDao.batchDelete(entityIdSet);
    }

    @Override
    protected void processAfterAllPageDone(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
    }

    @Override
    protected Pair<BusinessType, ToolType> getBusinessTypeToolTypePair() {
        return Pair.of(BusinessType.REVERT_IGNORE, ToolType.STANDARD);
    }
}
