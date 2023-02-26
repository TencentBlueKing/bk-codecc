package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.devops.common.constant.ComConstants.BusinessType;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 批量忽略的处理器
 *
 * @version V1.0
 * @date 2019/10/31
 */
@Service("CCNBatchMarkDefectBizService")
public class CCNBatchMarkDefectBizServiceImpl extends AbstractCCNBatchDefectProcessBizService
{
    @Autowired
    private CCNDefectDao defectDao;

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        defectDao.batchMarkDefect(batchDefectProcessReqVO.getTaskId(), defectList,
                batchDefectProcessReqVO.getMarkFlag());
    }

    @Override
    protected void doBizByPage(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        defectDao.batchMarkDefect(batchDefectProcessReqVO.getTaskId(), defectList,
                batchDefectProcessReqVO.getMarkFlag());
    }


    @Override
    protected void processAfterAllPageDone(BatchDefectProcessReqVO batchDefectProcessReqVO) {

    }

    @Override
    protected Pair<BusinessType, ToolType> getBusinessTypeToolTypePair() {
        return Pair.of(BusinessType.MARK_DEFECT, ToolType.CCN);
    }
}
