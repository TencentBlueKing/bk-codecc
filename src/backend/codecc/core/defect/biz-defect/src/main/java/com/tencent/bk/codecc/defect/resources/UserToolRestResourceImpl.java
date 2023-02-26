package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.UserToolRestResource;
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@RestResource
public class UserToolRestResourceImpl implements UserToolRestResource {

    @Autowired
    private TaskLogOverviewService taskLogOverviewService;

    @Override
    public Result<List<String>> getLastAnalyzeTool(String pipelineId, String multiPipelineMark) {
        return new Result<>(taskLogOverviewService.getLastAnalyzeTool(pipelineId, multiPipelineMark));
    }
}
