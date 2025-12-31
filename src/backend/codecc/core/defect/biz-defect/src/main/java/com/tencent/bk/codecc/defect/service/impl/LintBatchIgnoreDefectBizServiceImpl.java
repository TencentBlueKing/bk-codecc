package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.IgnoredNegativeDefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.service.IIgnoredNegativeDefectService;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BusinessType;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import java.util.List;
import java.util.Set;

import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

/**
 * Lint类工具批量忽略的处理器
 *
 * @version V1.0
 * @date 2020/3/2
 */
@Slf4j
@Service("LINTBatchIgnoreDefectBizService")
public class LintBatchIgnoreDefectBizServiceImpl extends AbstractLintBatchDefectProcessBizService {

    @Autowired
    private LintDefectV2Dao defectDao;

    @Autowired
    private IgnoredNegativeDefectDao ignoredNegativeDefectDao;

    @Autowired
    private IIgnoredNegativeDefectService iIgnoredNegativeDefectService;

    @Autowired
    private Client client;

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        int status = ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value();
        defectList.forEach(defectEntity -> ((LintDefectV2Entity) defectEntity).setStatus(status));
        defectDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defectList,
                batchDefectProcessReqVO.getIgnoreReasonType(),
                batchDefectProcessReqVO.getIgnoreReason(),
                batchDefectProcessReqVO.getIgnoreAuthor());

        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
    }

    @Override
    protected Set<String> getStatusCondition(DefectQueryReqVO queryCondObj) {
        // 对已经修复的BUG也可以进行忽略
        return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value()),
                String.valueOf(ComConstants.DefectStatus.FIXED.value()));
    }

    @Override
    protected void doBizByPage(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        int status = ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value();
        defectList.forEach(defectEntity -> ((LintDefectV2Entity) defectEntity).setStatus(status));
        defectDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defectList,
                batchDefectProcessReqVO.getIgnoreReasonType(),
                batchDefectProcessReqVO.getIgnoreReason(), batchDefectProcessReqVO.getIgnoreAuthor());

        if (batchDefectProcessReqVO.getIgnoreReasonType() == ComConstants.IgnoreReasonType.ERROR_DETECT.value()) {
            Result<TaskDetailVO> taskBaseResult = client.get(ServiceTaskRestResource.class)
                    .getTaskInfoById(batchDefectProcessReqVO.getTaskId());
            if (null == taskBaseResult || taskBaseResult.isNotOk() || null == taskBaseResult.getData()) {
                log.error("get task info fail!, task id: {}", batchDefectProcessReqVO.getTaskId());
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }

            iIgnoredNegativeDefectService.batchInsertIgnoredDefects(defectList,
                    batchDefectProcessReqVO, taskBaseResult.getData());
        }
    }

    @Override
    protected void processAfterAllPageDone(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
    }

    @Override
    protected Pair<BusinessType, ToolType> getBusinessTypeToolTypePair() {
        return Pair.of(BusinessType.IGNORE_DEFECT, ToolType.STANDARD);
    }
}
