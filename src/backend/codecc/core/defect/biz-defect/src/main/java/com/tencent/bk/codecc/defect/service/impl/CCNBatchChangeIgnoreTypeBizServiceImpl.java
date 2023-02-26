package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BusinessType;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 圈复杂度改变忽略类型
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
@Service("CCNBatchChangeIgnoreTypeBizService")
public class CCNBatchChangeIgnoreTypeBizServiceImpl extends AbstractCCNUpdateDefectStatusService {
    /**
     * 获取批处理类型对应的告警状态条件
     * 忽略告警、告警处理人分配、告警标志修改针对的都是待修复告警，而改变忽略类型针对的是已忽略告警
     * @param queryCondObj
     * @return
     */
    @Override
    protected Set<String> getStatusCondition(DefectQueryReqVO queryCondObj) {
        return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.IGNORE.value()));
    }

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        updateDefects(defectList, batchDefectProcessReqVO);
    }

    @Override
    protected void doBizByPage(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        updateDefects(defectList, batchDefectProcessReqVO);
    }

    @Override
    protected void processAfterAllPageDone(BatchDefectProcessReqVO batchDefectProcessReqVO) {

    }

    @Override
    protected Pair<BusinessType, ToolType> getBusinessTypeToolTypePair() {
        return Pair.of(BusinessType.CHANGE_IGNORE_TYPE, ToolType.CCN);
    }

    @Override
    protected void updateDefectStatus(CCNDefectEntity defectEntity, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        defectEntity.setIgnoreReasonType(batchDefectProcessReqVO.getIgnoreReasonType());
        defectEntity.setIgnoreReason(batchDefectProcessReqVO.getIgnoreReason());
    }
}
