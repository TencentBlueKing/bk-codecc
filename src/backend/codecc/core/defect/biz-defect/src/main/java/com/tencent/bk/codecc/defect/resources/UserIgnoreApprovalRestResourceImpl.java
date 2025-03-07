package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.UserIgnoreApprovalRestResource;
import com.tencent.bk.codecc.defect.service.IgnoreApprovalService;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreApprovalConfigVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.pojo.external.ResourceType;
import com.tencent.devops.common.auth.api.pojo.external.UserGroupRole;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.security.AuthMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@RestResource
public class UserIgnoreApprovalRestResourceImpl implements UserIgnoreApprovalRestResource {

    @Autowired
    private IgnoreApprovalService ignoreApprovalService;


    @Override
    @AuthMethod(resourceType = ResourceType.PROJECT, permission = {}, roles = UserGroupRole.MANAGER)
    public Result<Boolean> saveConfig(String projectId, String userName,
            IgnoreApprovalConfigVO ignoreApprovalConfigVO) {
        return new Result<>(ignoreApprovalService.savaApprovalConfig(projectId, userName, ignoreApprovalConfigVO));
    }

    @Override
    public Result<Page<IgnoreApprovalConfigVO>> configList(String projectId, String userName, Integer pageNum,
            Integer pageSize) {
        return new Result<>(ignoreApprovalService.projectConfigList(projectId, userName, pageNum, pageSize));
    }

    @Override
    public Result<IgnoreApprovalConfigVO> configDetail(String projectId, String userName, String ignoreApprovalId) {
        return new Result<>(ignoreApprovalService.approvalConfigDetail(projectId, userName, ignoreApprovalId));
    }

    @Override
    @AuthMethod(resourceType = ResourceType.PROJECT, permission = {}, roles = UserGroupRole.MANAGER)
    public Result<Boolean> configDelete(String projectId, String userName, String ignoreApprovalId) {
        return new Result<>(ignoreApprovalService.approvalConfigDelete(projectId, userName, ignoreApprovalId));
    }
}
