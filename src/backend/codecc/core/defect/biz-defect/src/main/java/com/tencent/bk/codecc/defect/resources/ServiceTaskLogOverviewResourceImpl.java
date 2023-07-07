package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceTaskLogOverviewResource;
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.defect.vo.TaskLogRepoInfoVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@RestResource
public class ServiceTaskLogOverviewResourceImpl implements ServiceTaskLogOverviewResource {

    @Autowired
    TaskLogOverviewService taskLogOverviewService;

    @Override
    public Result<TaskLogOverviewVO> getTaskLogOverview(Long taskId, String buildId, Integer status) {
        return new Result<>(taskLogOverviewService.getTaskLogOverview(taskId, buildId, status));
    }

    @Override
    public Result<TaskLogOverviewVO> getAnalyzeResult(Long taskId, String buildId, String buildNum, Integer status) {
        return new Result<>(taskLogOverviewService.getAnalyzeResult(taskId, buildId, buildNum, status));
    }

    @Override
    public Result<Integer> getTaskAnalyzeCount(@NotNull QueryTaskListReqVO reqVO) {
        return new Result<>(taskLogOverviewService
                .statTaskAnalyzeCount(reqVO.getTaskIds(), reqVO.getStatus(), reqVO.getStartTime(), reqVO.getEndTime()));
    }

    @Override
    public Result<List<String>> getLastAnalyzeTool(Long taskId) {
        return new Result<>(taskLogOverviewService.getLastAnalyzeTool(taskId));
    }

    @Override
    public Result<Map<Long, Map<String, TaskLogRepoInfoVO>>> batchGetLastAnalyzeRepoInfo(List<Long> taskIdList) {
        return new Result<>(taskLogOverviewService.batchGetRepoInfo(taskIdList));
    }
}
