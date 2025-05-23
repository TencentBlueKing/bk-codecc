package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.service.ISCABatchDefectProcessBizService;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.List2StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;


@Slf4j
@Service("SCABatchAssignDefectBizService")
public class SCABatchAssignDefectBizServiceImpl extends AbstractSCABatchDefectProcessBizService {

    @Autowired
    private BizServiceFactory<ISCABatchDefectProcessBizService> scaBatchDefectProcessBizServiceFactory;

    @Override
    protected List<Long> getTaskIdsByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        return Collections.emptyList();
    }

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        doBizByPage(defectList, batchDefectProcessReqVO);
        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
    }

    @Override
    protected void doBizByPage(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        String scaDimension = batchDefectProcessReqVO.getScaDimension();
        if (StringUtils.isEmpty(scaDimension)) {
            log.error("parameter [scaDimension] can't be empty");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{scaDimension}, null);
        }
        // 根据SCA维度,批量忽略告警
        ISCABatchDefectProcessBizService service = scaBatchDefectProcessBizServiceFactory.createBizService(
                Collections.emptyList(),
                Collections.singletonList(ComConstants.ToolType.SCA.name()),
                ComConstants.BizServiceFlag.CORE,
                StringUtils.capitalize(scaDimension.toLowerCase(Locale.ENGLISH))
                        + ComConstants.BATCH_DEFECT_PROCESSOR_INFFIX,
                ISCABatchDefectProcessBizService.class
        );
        Set<String> newAuthor = batchDefectProcessReqVO.getNewAuthor();
        if (CollectionUtils.isEmpty(newAuthor)) {
            log.error("parameter [newAuthor] can't be empty");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{newAuthor.toString()}, null);
        }

        service.batchUpdateDefectAuthor(batchDefectProcessReqVO.getTaskId(), defectList, newAuthor);
    }

    @Override
    protected void processCustomizeOpsAfterEachPageDone(List defectList,
                                                        BatchDefectProcessReqVO batchDefectProcessReqVO) {

    }

    @Override
    protected void processAfterAllPageDone(BatchDefectProcessReqVO batchDefectProcessReqVO) {

    }

    @Override
    protected List getDefectsByQueryCond(long taskId, DefectQueryReqVO defectQueryReqVO, Set<String> defectKeySet) {
        return Collections.emptyList();
    }

    @Override
    protected Pair<ComConstants.BusinessType, ComConstants.ToolType> getBusinessTypeToolTypePair() {
        return Pair.of(ComConstants.BusinessType.ASSIGN_DEFECT, ComConstants.ToolType.SCA);
    }
}