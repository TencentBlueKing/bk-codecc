package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.defect.vo.TaskLogRepoInfoVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.annotation.ServiceInterface;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "BUILD_CHECKER", description = "工具执行记录接口")
@Path("/service/taskLogOverview")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface(value = "defect")
public interface ServiceTaskLogOverviewResource {
    @Operation(summary = "获取工具记录")
    @Path("/")
    @GET
    Result<TaskLogOverviewVO> getTaskLogOverview(
            @Parameter(description = "任务ID", required = true)
            @HeaderParam("taskId")
                    Long taskId,
            @Parameter(description = "构建号")
            @QueryParam("buildId")
                    String buildId,
            @Parameter(description = "任务状态")
            @QueryParam("status")
                    Integer status
    );

    @Operation(summary = "获取扫描记录")
    @Path("/get")
    @GET
    Result<TaskLogOverviewVO> getTaskLogOverview(
            @Parameter(description = "任务ID", required = true)
            @HeaderParam("taskId")
            Long taskId,
            @Parameter(description = "构建号")
            @QueryParam("buildId")
            String buildId
    );

    @Operation(summary = "获取工具记录")
    @Path("/analyze/result")
    @GET
    Result<TaskLogOverviewVO> getAnalyzeResult(
            @Parameter(description = "任务ID", required = true)
            @HeaderParam("taskId")
                    Long taskId,
            @Parameter(description = "构建号")
            @QueryParam("buildId")
                    String buildId,
            @Parameter(description = "构建号")
            @QueryParam("buildNum")
                    String buildNum,
            @Parameter(description = "构建号")
            @QueryParam("status")
                    Integer status
    );

    @Operation(summary = "获取任务分析次数")
    @Path("/analyze/count")
    @POST
    Result<Integer> getTaskAnalyzeCount(
            @Parameter(description = "请求体")
            @Valid QueryTaskListReqVO reqVO
    );

    @Operation(summary = "获取最后构建的工具")
    @Path("/task/{taskId}/getLastAnalyzeTool")
    @GET
    Result<List<String>> getLastAnalyzeTool(
        @Parameter(description = "任务ID", required = true)
        @QueryParam("taskId")
            Long taskId
    );

    @Operation(summary = "批量获取最新分析记录")
    @Path("/batch/latest/repo")
    @POST
    Result<Map<Long, Map<String, TaskLogRepoInfoVO>>> batchGetLastAnalyzeRepoInfo(
            @Parameter(description = "任务ID", required = true)
                    List<Long> taskIdList);

    @Operation(summary = "获取最后构建ID")
    @Path("/analyze/last/buildId")
    @POST
    Result<Map<Long, String>> getLastAnalyzeBuildIdMap(
            @Parameter(description = "任务ID", required = true)
            Map<Long, Set<String>> taskIdToBuildIds
    );
}
