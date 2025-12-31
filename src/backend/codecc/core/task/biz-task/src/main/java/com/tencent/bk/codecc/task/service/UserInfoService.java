package com.tencent.bk.codecc.task.service;

import com.tencent.devops.common.api.OrgInfoVO;

/**
 * 用户信息查询处理
 */
public interface UserInfoService {

    OrgInfoVO getUserOrgInfo(String userName);

    String getUserDirectLeader(String userName);
}
