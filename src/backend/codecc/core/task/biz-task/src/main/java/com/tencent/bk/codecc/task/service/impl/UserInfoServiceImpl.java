package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.service.UserInfoService;
import com.tencent.devops.common.api.OrgInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.tencent.devops.common.constant.ComConstants.DEFAULT_BG_ID;


@Slf4j
@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Override
    public OrgInfoVO getUserOrgInfo(String userName) {
        return new OrgInfoVO();
    }

    @Override
    public String getUserDirectLeader(String userName) {
        return null;
    }
}
