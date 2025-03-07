package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.dao.SCAQueryWarningParams;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectQueryReqVO;

import java.util.List;
import java.util.Set;

/**
 * SCA不同维度告警处理接口，包括不同业务类型的处理
 */
public interface ISCABatchDefectProcessBizService {
    List getDefectsByQueryCondWithPage(
            SCAQueryWarningParams scaQueryWarningParams,
            String entityId,
            Integer pageSize
    );

    List getEffectiveDefectByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO);

    void batchIgnoreDefect(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO);

    void batchIgnoreDefectByPage(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO);

    void batchUpdateIgnoreType(
            long taskId,
            List<LintDefectV2Entity> defectList,
            int ignoreReasonType,
            String ignoreReason
    );

    void batchMarkDefect(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO);

    void batchRevertIgnore(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO);

    void batchUpdateDefectAuthor(long taskId, List defectList, Set<String> newAuthor);
}
