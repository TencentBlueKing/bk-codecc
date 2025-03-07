package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.IgnoredNegativeDefectDao;
import com.tencent.bk.codecc.defect.service.ISCABatchDefectProcessBizService;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 批处理变更忽略规则类型变更业务逻辑实现类
 * @author: lanazeng
 * @date: 2022/09/23 11:30
 */
@Slf4j
@Service("SCABatchChangeIgnoreTypeBizService")
public class SCABatchChangeIgnoreTypeBizServiceImpl extends AbstractSCABatchDefectProcessBizService {

    @Autowired
    private BizServiceFactory<ISCABatchDefectProcessBizService> scaBatchDefectProcessBizServiceFactory;

    @Autowired
    private IgnoredNegativeDefectDao ignoredNegativeDefectDao;

    @Override
    protected List<Long> getTaskIdsByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        return Collections.emptyList();
    }

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
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

        service.batchUpdateIgnoreType(
                batchDefectProcessReqVO.getTaskId(),
                defectList,
                batchDefectProcessReqVO.getIgnoreReasonType(),
                batchDefectProcessReqVO.getIgnoreReason()
        );

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

        service.batchUpdateIgnoreType(
                batchDefectProcessReqVO.getTaskId(),
                defectList,
                batchDefectProcessReqVO.getIgnoreReasonType(),
                batchDefectProcessReqVO.getIgnoreReason()
        );

        if (batchDefectProcessReqVO.getIgnoreReasonType() != ComConstants.IgnoreReasonType.ERROR_DETECT.value()) {
            ignoredNegativeDefectDao.batchDelete(batchDefectProcessReqVO.getDefectKeySet());
        }
    }

    @Override
    protected void processCustomizeOpsAfterEachPageDone(
            List defectList,
            BatchDefectProcessReqVO batchDefectProcessReqVO
    ) {}

    @Override
    protected void processAfterAllPageDone(BatchDefectProcessReqVO batchDefectProcessReqVO) {}

    @Override
    protected List getDefectsByQueryCond(long taskId, DefectQueryReqVO defectQueryReqVO, Set<String> defectKeySet) {
        return Collections.emptyList();
    }
}
