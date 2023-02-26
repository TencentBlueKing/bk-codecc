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
 * 圈复杂度忽略告警实现类
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
@Service("CCNBatchIgnoreDefectBizService")
public class CCNBatchIgnoreDefectBizServiceImpl extends AbstractCCNUpdateDefectStatusService {
    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        updateDefects(defectList, batchDefectProcessReqVO);
    }

    @Override
    protected void doBizByPage(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        updateDefects(defectList, batchDefectProcessReqVO);
    }

    @Override
    protected void processAfterEachPageDone(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {

    }

    @Override
    protected void processAfterAllPageDone(BatchDefectProcessReqVO batchDefectProcessReqVO) {

    }

    @Override
    protected void updateDefectStatus(CCNDefectEntity defectEntity, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        int status = ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value();
        defectEntity.setStatus(status);
        defectEntity.setIgnoreTime(System.currentTimeMillis());
        defectEntity.setIgnoreAuthor(batchDefectProcessReqVO.getIgnoreAuthor());
        defectEntity.setIgnoreReason(batchDefectProcessReqVO.getIgnoreReason());
        defectEntity.setIgnoreReasonType(batchDefectProcessReqVO.getIgnoreReasonType());
    }

    @Override
    protected Set<String> getStatusCondition(DefectQueryReqVO queryCondObj) {
        //对已经修复的BUG也可以进行忽略
        return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value()),
                String.valueOf(ComConstants.DefectStatus.FIXED.value()));
    }

    @Override
    protected Pair<BusinessType, ToolType> getBusinessTypeToolTypePair() {
        return Pair.of(BusinessType.IGNORE_DEFECT, ToolType.CCN);
    }
}
