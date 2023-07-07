package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.devops.common.constant.ComConstants;
import java.util.List;
import java.util.Set;

public interface BatchDefectProcessHandler {

    Set<ComConstants.BusinessType> supportBusinessTypes();

    Set<ComConstants.ToolType> supportToolTypes();

    void handler(List defect, BatchDefectProcessReqVO batchDefectProcessReqVO,
            ComConstants.BusinessType type, ComConstants.ToolType toolType);

}
