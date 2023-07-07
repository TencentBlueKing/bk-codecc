package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongotemplate.DefectDao;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BusinessType;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

/**
 * 批量忽略的处理器
 *
 * @version V1.0
 * @date 2019/10/31
 */
@Slf4j
@Service("CommonBatchIgnoreDefectBizService")
public class CommonBatchIgnoreDefectBizServiceImpl extends AbstractCommonBatchDefectProcessBizService {

    @Autowired
    private DefectDao defectDao;

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        int status = ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value();
        defectList.forEach(defectEntity -> ((CommonDefectEntity) defectEntity).setStatus(status));
        defectDao.batchUpdateDefectStatusIgnoreBit(
                batchDefectProcessReqVO.getTaskId(),
                defectList,
                batchDefectProcessReqVO.getIgnoreReasonType(),
                batchDefectProcessReqVO.getIgnoreReason(),
                batchDefectProcessReqVO.getIgnoreAuthor()
        );

        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
    }

    @Override
    protected Set<String> getStatusCondition(DefectQueryReqVO queryCondObj) {
        // 已修复在快照查中可能是未修复
        return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value()),
                String.valueOf(ComConstants.DefectStatus.FIXED.value()));
    }

    @Override
    protected Pair<BusinessType, ToolType> getBusinessTypeToolTypePair() {
        return Pair.of(BusinessType.IGNORE_DEFECT, ToolType.DEFECT);
    }
}
