package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Tag(name = "SERVICE_TOOLCONFIG", description = "任务-工具映射管理接口")
@Path("/service/toolConfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceToolConfigRestResource {

    @Operation(summary = "获取任务总数")
    @Path("/")
    @GET
    Result<List<ToolConfigInfoVO>> getByTaskId(
            @Parameter(description = "任务ID")
            @QueryParam("taskId")
            Long taskId
    );

    @Operation(summary = "根据任务列表与工具名获取工具配置")
    @Path("/getByTaskIdsAndToolName")
    @GET
    Result<List<ToolConfigInfoVO>> getByTaskIdsAndToolName(
            @Parameter(description = "任务ID列表")
            @QueryParam("taskIds")
            List<Long> taskIds,
            @Parameter(description = "工具名称")
            @QueryParam("toolName")
            String toolName
    );

    @Operation(summary = "根据任务列表获取所有工具配置")
    @Path("/getByTaskIds")
    @GET
    Result<List<ToolConfigInfoVO>> getByTaskIds(
            @Parameter(description = "任务ID列表")
            @QueryParam("taskIds")
            List<Long> taskIds
    );

    @Operation(summary = "根据任务列表获取所有工具配置")
    @Path("/getByToolWithCursor")
    @GET
    Result<List<Long>> getTaskIdsByToolWithCursor(
            @Parameter(description = "工具名")
            @QueryParam("toolName")
            String toolName,
            @Parameter(description = "任务ID")
            @QueryParam("taskId")
            Long taskId,
            @Parameter(description = "每页大小")
            @QueryParam("size")
            Long size
    );
}
