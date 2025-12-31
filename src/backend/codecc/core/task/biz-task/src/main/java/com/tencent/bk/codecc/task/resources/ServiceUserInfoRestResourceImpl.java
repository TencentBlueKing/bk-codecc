package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.ServiceUserInfoRestResource;
import com.tencent.bk.codecc.task.service.UserInfoService;
import com.tencent.devops.common.api.OrgInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@RestResource
public class ServiceUserInfoRestResourceImpl implements ServiceUserInfoRestResource {

    @Autowired
    private UserInfoService userInfoService;

    @Override
    public Result<OrgInfoVO> getOrgInfo(String userId) {
        return new Result<>(userInfoService.getUserOrgInfo(userId));
    }

    @Override
    public Result<String> getUserDirectLeader(String userId) {
        return new Result<>(userInfoService.getUserDirectLeader(userId));
    }
}
