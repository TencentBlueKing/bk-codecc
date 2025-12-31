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
    public Result<List<ToolConfigInfoVO>> getByTaskId(Long taskId) {
        return new Result<>(toolConfigService.getToolConfigByTaskId(taskId));
    }

    @Override
    public Result<List<ToolConfigInfoVO>> getByTaskIdsAndToolName(List<Long> taskIds, String toolName) {
        return new Result<>(toolConfigService.getToolConfigByTaskIdAndToolName(taskIds, toolName));
    }

    @Override
    public Result<List<ToolConfigInfoVO>> getByTaskIds(List<Long> taskIds) {
        return new Result<>(toolConfigService.getToolConfigByTaskIdIn(taskIds));
    }

    @Override
    public Result<List<Long>> getTaskIdsByToolWithCursor(String toolName, Long taskId, Long size) {
        return new Result<>(toolConfigService.getTaskIdsByToolWithCursor(toolName, taskId, size));
    }
}
