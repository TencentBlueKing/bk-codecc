package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongotemplate.DefectDao;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.devops.common.constant.ComConstants.BusinessType;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
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
@Service("CommonBatchMarkDefectBizService")
public class CommonBatchMarkDefectBizServiceImpl extends AbstractCommonBatchDefectProcessBizService {

    @Autowired
    private DefectDao defectDao;

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        defectDao.batchMarkDefect(batchDefectProcessReqVO.getTaskId(), defectList,
                batchDefectProcessReqVO.getMarkFlag());
    }

    @Override
    protected Set<String> getStatusCondition(DefectQueryReqVO queryCondObj) {
        // 对于恢复忽略再标记的需要开放忽略
        if (queryCondObj != null && queryCondObj.getRevertAndMark() != null && queryCondObj.getRevertAndMark()) {
            return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value()),
                    String.valueOf(DefectStatus.IGNORE.value()));
        }
        return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value()));
    }

    @Override
    protected Pair<BusinessType, ToolType> getBusinessTypeToolTypePair() {
        return Pair.of(BusinessType.MARK_DEFECT, ToolType.DEFECT);
    }
}
