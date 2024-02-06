package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.DefectDao;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BusinessType;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

/**
 * 批量忽略的处理器
 *
 * @version V1.0
 * @date 2019/10/31
 */
@Service("CommonBatchRevertIgnoreBizService")
public class CommonBatchRevertIgnoreBizServiceImpl extends AbstractCommonBatchDefectProcessBizService {

    @Autowired
    private DefectDao defectDao;

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
        defectList.forEach(defectEntity -> {
            CommonDefectEntity defect = (CommonDefectEntity) defectEntity;
            defect.setStatus(defect.getStatus() - ComConstants.DefectStatus.IGNORE.value());
        });

        defectDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defectList, 0, null, null);
        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
    }

    @Override
    protected Pair<BusinessType, ToolType> getBusinessTypeToolTypePair() {
        return Pair.of(BusinessType.REVERT_IGNORE, ToolType.DEFECT);
    }
}
