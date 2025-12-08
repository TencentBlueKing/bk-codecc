package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.ToolBuildStackReqVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * 工具构建信息接口
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Tag(name = "SERVICE_TOOL_BUILD_INFO", description = "工具构建信息接口")
@Path("/service/toolBuildInfo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceToolBuildInfoResource {
    @Operation(summary = "设置强制全量扫描标志位")
    @Path("/task/{taskId}/forceFullScanSymbol")
    @POST
    Result<Boolean> setForceFullScan(
            @Parameter(description = "任务ID", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "任务id及工具集映射参数", required = true)
                    List<String> toolNames);

    @Operation(summary = "设置运行时栈强制全量扫描标志位")
    @Path("/stack/task/{taskId}/forceFullScan")
    @POST
    Result<Boolean> setToolBuildStackFullScan(
            @Parameter(description = "任务ID", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "任务id及工具集映射参数", required = true)
                    ToolBuildStackReqVO toolBuildStackReqVO);

    @Operation(summary = "批量设置强制全量扫描标志位")
    @Path("/batch/forceFullScan")
    @POST
    Result<Boolean> batchSetForceFullScan(
            @Parameter(description = "任务ID", required = true) @Valid
                    QueryTaskListReqVO reqVO
    );

    @Operation(summary = "设置运行时栈增量扫描时间位")
    @Path("/stack/task/{taskId}/commitSince/set")
    @POST
    Result<Boolean> setToolBuildStackCommitSince(
            @Parameter(description = "任务ID", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "任务id及工具集映射参数", required = true)
                    ToolBuildStackReqVO toolBuildStackReqVO);

    @Operation(summary = "设置运行时栈增量扫描时间位")
    @Path("/stack/task/{taskId}/commitSince/get")
    @POST
    Result<Long> getToolBuildStackCommitSince(
            @Parameter(description = "任务ID", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "任务id及工具集映射参数", required = true)
                    ToolBuildStackReqVO toolBuildStackReqVO);


    @Operation(summary = "设置运行时栈全量扫描标志位")
    @Path("/stack/task/{taskId}/notFullScanIfRebuildIncr")
    @POST
    Result<Boolean> setToolBuildStackNotFullScanIfRebuildIncr(
            @Parameter(description = "任务ID", required = true)
            @PathParam("taskId")
                    Long  taskId,
            @Parameter(description = "任务id及工具集映射参数", required = true)
                    ToolBuildStackReqVO toolBuildStackReqVO);
}
