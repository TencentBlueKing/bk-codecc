package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.UserIgnoreTypeRestResource;
import com.tencent.bk.codecc.defect.service.IIgnoreTypeService;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeDefectStatResponse;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeProjectConfigVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeSysVO;
import com.tencent.devops.common.api.annotation.I18NResponse;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.auth.api.pojo.external.ResourceType;
import com.tencent.devops.common.web.RestResource;
import java.util.List;

import com.tencent.devops.common.web.security.AuthMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@RestResource
public class UserIgnoreTypeRestResourceImpl implements UserIgnoreTypeRestResource {

    @Autowired
    private IIgnoreTypeService iIgnoreTypeService;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Override
    @AuthMethod(resourceType = ResourceType.PROJECT, permission = {CodeCCAuthAction.IGNORE_TYPE_MANAGE})
    public Result<Boolean> save(String projectId, String userName, IgnoreTypeProjectConfigVO projectConfig) {
        return new Result<>(iIgnoreTypeService.ignoreTypeProjectSave(projectId, userName, projectConfig));
    }

    @Override
    @AuthMethod(resourceType = ResourceType.PROJECT, permission = {CodeCCAuthAction.IGNORE_TYPE_MANAGE})
    public Result<Boolean> updateStatus(String projectId, String userName, IgnoreTypeProjectConfigVO projectConfig) {
        return new Result<>(iIgnoreTypeService.updateIgnoreTypeProjectStatus(projectId, userName, projectConfig));
    }

    @Override
    public Result<List<IgnoreTypeSysVO>> queryIgnoreTypeSysList() {
        return new Result<>(iIgnoreTypeService.queryIgnoreTypeSysList());
    }

    @Override
    @I18NResponse
    public Result<List<IgnoreTypeProjectConfigVO>> list(String projectId, String userName) {
        return new Result<>(iIgnoreTypeService.queryIgnoreTypeProjectList(projectId, userName));
    }



    @Override
    @I18NResponse
    public Result<IgnoreTypeProjectConfigVO> detail(String projectId, String userName, Integer ignoreTypeId) {
        return new Result<>(iIgnoreTypeService.ignoreTypeProjectDetail(projectId, userName, ignoreTypeId));
    }

    @Override
    public Result<List<IgnoreTypeDefectStatResponse>> defectStat(String projectId, String userName) {
        return new Result<>(iIgnoreTypeService.getIgnoreTypeDefectStat(projectId, userName, null));
    }

    @Override
    public Result<Boolean> hasAddPermissions(String projectId, String userName) {
        boolean projectManager = authExPermissionApi.authProjectMultiManager(projectId, userName);
        return new Result<>(projectManager);
    }

}
