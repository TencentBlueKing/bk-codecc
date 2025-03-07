package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.ignore.BgSecurityApprovalVO;
import com.tencent.devops.common.api.OrgInfoVO;

import java.util.List;

/**
 * BG安全审批人接口
 */
public interface BgSecurityApprovalService {

    /**
     * 获取BG审批人留列表
     *
     * @param projectScopeType 项目范围类型
     * @return list
     */
    List<BgSecurityApprovalVO> bgSecurityApprovalList(String projectScopeType, OrgInfoVO orgInfo);

    /**
     * 插入/更新忽略审批人配置
     *
     * @param reqVO 请求体
     * @return boolean
     */
    Boolean upsertBgApprovalConfig(BgSecurityApprovalVO reqVO);

    /**
     * 删除审批人配置
     * @param entityId 实体id
     * @return boolean
     */
    Boolean deleteBgApprovalConfig(String entityId, String userId);
}
