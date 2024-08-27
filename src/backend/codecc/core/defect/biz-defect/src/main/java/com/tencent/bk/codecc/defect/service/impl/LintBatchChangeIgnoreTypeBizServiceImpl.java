package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.IgnoredNegativeDefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
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
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lint工具批量恢复忽略实现类
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
@Service("LINTBatchChangeIgnoreTypeBizService")
public class LintBatchChangeIgnoreTypeBizServiceImpl extends AbstractLintBatchDefectProcessBizService {
    @Autowired
    private LintDefectV2Dao defectDao;

    @Autowired
    private IgnoredNegativeDefectDao ignoredNegativeDefectDao;

    @Autowired
    private Client client;

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
        defectDao.batchUpdateIgnoreType(batchDefectProcessReqVO.getTaskId(), defectList,
                batchDefectProcessReqVO.getIgnoreReasonType(), batchDefectProcessReqVO.getIgnoreReason());

        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
    }

    @Override
    protected void doBizByPage(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        defectDao.batchUpdateIgnoreType(batchDefectProcessReqVO.getTaskId(), defectList,
                batchDefectProcessReqVO.getIgnoreReasonType(),
                batchDefectProcessReqVO.getIgnoreReason());

        if (batchDefectProcessReqVO.getIgnoreReasonType() == ComConstants.IgnoreReasonType.ERROR_DETECT.value()) {
            Result<TaskDetailVO> taskBaseResult = client.get(ServiceTaskRestResource.class)
                    .getTaskInfoById(batchDefectProcessReqVO.getTaskId());
            if (null == taskBaseResult || taskBaseResult.isNotOk() || null == taskBaseResult.getData()) {
                log.error("get task info fail!, task id: {}", batchDefectProcessReqVO.getTaskId());
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }

            ignoredNegativeDefectDao.batchInsert(defectList, batchDefectProcessReqVO, taskBaseResult.getData());
        } else {
            ignoredNegativeDefectDao.batchDelete(batchDefectProcessReqVO.getDefectKeySet());
        }
    }

    @Override
    protected Pair<BusinessType, ToolType> getBusinessTypeToolTypePair() {
        return Pair.of(BusinessType.CHANGE_IGNORE_TYPE, ToolType.STANDARD);
    }
}
