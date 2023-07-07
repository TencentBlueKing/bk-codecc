package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceIgnoreTypeRestResource;
import com.tencent.bk.codecc.defect.service.IIgnoreTypeService;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeProjectConfigVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@RestResource
public class ServiceIgnoreTypeRestResourceImpl implements ServiceIgnoreTypeRestResource {

    @Autowired
    private IIgnoreTypeService iIgnoreTypeService;

    @Override
    public Result<IgnoreTypeProjectConfigVO> detail(String projectId, String userName, Integer ignoreTypeId) {
        return new Result<>(iIgnoreTypeService.ignoreTypeProjectDetail(projectId, userName, ignoreTypeId));
    }

}
