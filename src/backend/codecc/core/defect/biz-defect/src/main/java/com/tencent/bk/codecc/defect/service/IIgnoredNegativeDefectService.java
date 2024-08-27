package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.IgnoredNegativeDefectStatisticVO;
import com.tencent.bk.codecc.defect.vo.IgnoredNegativeDefectVO;
import com.tencent.bk.codecc.defect.vo.ListNegativeDefectReqVO;
import com.tencent.bk.codecc.defect.vo.ProcessNegativeDefectReqVO;

import java.util.List;

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
}
