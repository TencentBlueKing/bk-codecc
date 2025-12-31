package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.BatchGetUserNameReqVO;
import com.tencent.bk.codecc.defect.vo.BatchGetUserNameRespVO;

import java.util.List;
import java.util.Set;

/**
 * 多租户辅助服务
 */
public interface MultiTenantService {
    BatchGetUserNameRespVO batchGetUserName(BatchGetUserNameReqVO request);

    // userId -> userName
    Set<String> transUserId(String tenantId, List<String> userIds);
}
