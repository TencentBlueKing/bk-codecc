package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.service.ToolBuildInfoService;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BusinessType;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 圈复杂度恢复忽略实现类
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
@Service("CCNBatchRevertIgnoreBizService")
public class CCNBatchRevertIgnoreBizServiceImpl extends AbstractCCNUpdateDefectStatusService {

    @Autowired
    private ToolBuildInfoService toolBuildInfoService;

    /**
     * 获取批处理类型对应的告警状态条件
     * 忽略告警、告警处理人分配、告警标志修改针对的都是待修复告警，而恢复忽略针对的是已忽略告警
     * @param queryCondObj
     * @return
     */
    @Override
    protected Set<String> getStatusCondition(DefectQueryReqVO queryCondObj) {
        return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.IGNORE.value()));
    }

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        updateDefects(defectList, batchDefectProcessReqVO);
        processRevertIgnoreOperation(batchDefectProcessReqVO);
    }

    @Override
    protected void doBizByPage(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        updateDefects(defectList, batchDefectProcessReqVO);
    }


    @Override
    protected void processAfterAllPageDone(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        processRevertIgnoreOperation(batchDefectProcessReqVO);
    }

    @Override
    protected void updateDefectStatus(CCNDefectEntity defectEntity, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        if ((defectEntity.getStatus() & ComConstants.DefectStatus.IGNORE.value()) > 0
                && (null == defectEntity.getIgnoreCommentDefect() || !defectEntity.getIgnoreCommentDefect())) {
            defectEntity.setStatus(defectEntity.getStatus() - ComConstants.DefectStatus.IGNORE.value());
            defectEntity.setIgnoreAuthor(null);
            defectEntity.setIgnoreReason(null);
            defectEntity.setIgnoreReasonType(0);
        }
    }

    @Override
    protected Pair<BusinessType, ToolType> getBusinessTypeToolTypePair() {
        return Pair.of(BusinessType.REVERT_IGNORE, ToolType.CCN);
    }


    /**
     * 处理恢复忽略操作，将缺陷列表转换为任务工具信息并设置到请求VO中
     *
     * @param batchDefectProcessReqVO 批量处理请求VO
     */
    private void processRevertIgnoreOperation(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        if (batchDefectProcessReqVO == null) {
            return;
        }
        List<Long> taskIds = batchDefectProcessReqVO.getTaskIdList();
        if (CollectionUtils.isEmpty(taskIds)) {
            return;
        }
        // 转换为TaskInfoVO列表
        List<BatchDefectProcessReqVO.TaskInfoVO> taskInfos = taskIds.stream()
                .map(taskId -> {
                    BatchDefectProcessReqVO.TaskInfoVO taskInfo = new BatchDefectProcessReqVO.TaskInfoVO();
                    taskInfo.setTaskId(taskId);
                    taskInfo.setToolNames(Sets.newHashSet(ComConstants.Tool.CCN.name()));
                    return taskInfo;
                })
                .collect(Collectors.toList());

        // 合并或设置任务信息到请求VO中
        if (CollectionUtils.isNotEmpty(batchDefectProcessReqVO.getTaskInfos())) {
            batchDefectProcessReqVO.getTaskInfos().addAll(taskInfos);
        } else {
            batchDefectProcessReqVO.setTaskInfos(taskInfos);
        }

        // 设置工具强制全量扫描
        toolBuildInfoService.batchSetForceFullScan(batchDefectProcessReqVO.getTaskInfos());
    }
}
