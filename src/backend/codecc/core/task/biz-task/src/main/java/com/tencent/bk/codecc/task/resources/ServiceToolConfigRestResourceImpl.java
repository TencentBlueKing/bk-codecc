package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.ServiceToolConfigRestResource;
import com.tencent.bk.codecc.task.service.ToolConfigService;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@RestResource
public class ServiceToolConfigRestResourceImpl implements ServiceToolConfigRestResource {

    @Autowired
    private ToolConfigService toolConfigService;

    @Override
    public Result<List<ToolConfigInfoVO>> getTaskIdByPage(Long taskId) {
        return new Result<>(toolConfigService.getToolConfigByTaskId(taskId));
    }
}
