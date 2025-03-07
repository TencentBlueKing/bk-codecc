package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.dao.SCAQueryWarningParams;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectDetailQueryReqVO;
import org.springframework.data.domain.Sort;


/**
 * SCA不同维度告警查询接口
 */
public interface ISCAQueryWarningService {
    /**
     * 处理SCA告警列表查询
     * @param scaQueryWarningParams
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortType
     * @return
     */
    CommonDefectQueryRspVO processQueryWarningRequest(
            SCAQueryWarningParams scaQueryWarningParams,
            int pageNum,
            int pageSize,
            String sortField,
            Sort.Direction sortType
    );

    /**
     * 处理SCA告警详情查询
     * @param requestVO
     * @return
     */
    CommonDefectDetailQueryRspVO processQueryWarningDetailRequest(
            SCADefectDetailQueryReqVO requestVO
    );

    /**
     * 处理SCA页面告警数统计数据
     * @param requestVO
     * @return
     */
    Object pageInit(SCAQueryWarningParams requestVO);

    QueryWarningPageInitRspVO processQueryAuthorsRequest(
            SCAQueryWarningParams scaQueryWarningParams
    );
}
