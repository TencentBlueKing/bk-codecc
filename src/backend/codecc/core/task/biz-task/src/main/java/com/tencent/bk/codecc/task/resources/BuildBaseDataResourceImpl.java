package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.BuildBaseDataResource;
import com.tencent.bk.codecc.task.service.BaseDataService;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@RestResource
public class BuildBaseDataResourceImpl implements BuildBaseDataResource {

    @Autowired
    private BaseDataService baseDataService;

    @Override
    public Result<List<BaseDataVO>> getParamsByType(String paramType) {
        return new Result(baseDataService.findBaseDataInfoByType(paramType));
    }
}
