package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceToolDeveloperInfoResource;
import com.tencent.bk.codecc.defect.service.ToolDeveloperInfoService;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 工具开发者信息实现类
 *
 * @version V1.0
 * @date 2025/8/6
 */
@Slf4j
@RestResource
public class ServiceToolDeveloperInfoResourceImpl implements ServiceToolDeveloperInfoResource {

    @Autowired
    private ToolDeveloperInfoService toolDeveloperInfoService;

    @Override
    public Result<Boolean> initializationToolDeveloper() {
        return new Result<>(toolDeveloperInfoService.initializationToolDeveloper());
    }
}
