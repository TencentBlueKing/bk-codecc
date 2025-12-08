package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.IgnoredNegativeDefectStatisticVO;
import com.tencent.bk.codecc.defect.vo.IgnoredNegativeDefectVO;
import com.tencent.bk.codecc.defect.vo.ListNegativeDefectReqVO;
import com.tencent.bk.codecc.defect.vo.ProcessNegativeDefectReqVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;

import java.util.List;
import java.util.Set;

/**
 * 以 "误报" 为理由被忽略的告警相关页面的 service 层
 *
 * @date 2024/3/7
 */
public interface IIgnoredNegativeDefectService {

    IgnoredNegativeDefectStatisticVO statistic(String toolName, Integer n);

    List<IgnoredNegativeDefectVO> listDefectAfterFilter(
            String toolName,
            Integer n,
            String lastInd,
            Integer pageSize,
            String orderBy,
            String orderDirection,
            ListNegativeDefectReqVO listNegativeDefectReq
    );

    Long countDefectAfterFilter(String toolName, Integer n, ListNegativeDefectReqVO listNegativeDefectReq);

    Boolean processNegativeDefect(String entityId, ProcessNegativeDefectReqVO processNegativeDefectReq);

    void batchDeleteIgnoredDefects(Long taskId, Set<String> defectIds);

    void batchInsertIgnoredDefects(List defects, BatchDefectProcessReqVO batchDefectProcessReq,
            TaskDetailVO taskDetail);
}
