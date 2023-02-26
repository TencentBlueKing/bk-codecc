package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.UserIgnoreTypeRestResource;
import com.tencent.bk.codecc.defect.service.IIgnoreTypeService;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeDefectStatResponse;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeProjectConfigVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeSysVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.web.RestResource;
import java.util.List;
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
    public Result<Boolean> save(String projectId, String userName, IgnoreTypeProjectConfigVO projectConfig) {
        return new Result<>(iIgnoreTypeService.ignoreTypeProjectSave(projectId, userName, projectConfig));
    }

    @Override
    public Result<Boolean> updateStatus(String projectId, String userName, IgnoreTypeProjectConfigVO projectConfig) {
        return new Result<>(iIgnoreTypeService.updateIgnoreTypeProjectStatus(projectId, userName, projectConfig));
    }

    @Override
    public Result<List<IgnoreTypeSysVO>> queryIgnoreTypeSysList() {
        return new Result<>(iIgnoreTypeService.queryIgnoreTypeSysList());
    }

    @Override
    public Result<List<IgnoreTypeProjectConfigVO>> list(String projectId, String userName) {
        return new Result<>(iIgnoreTypeService.queryIgnoreTypeProjectList(projectId, userName));
    }



    @Override
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
