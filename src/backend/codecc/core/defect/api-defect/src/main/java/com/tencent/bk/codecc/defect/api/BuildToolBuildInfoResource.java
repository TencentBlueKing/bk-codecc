package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.ToolBuildStackReqVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * 工具构建信息接口
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Tag(name = "BUILD_TOOL_BUILD_INFO", description = "工具构建信息接口")
@Path("/build/toolBuildInfo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildToolBuildInfoResource
{
    @Operation(summary = "设置强制全量扫描标志位")
    @Path("/tasks/{taskId}/forceFullScanSymbol")
    @POST
    Result<Boolean> setToolBuildStackFullScan(
            @Parameter(description = "任务ID", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "任务id及工具集映射参数", required = true)
                    ToolBuildStackReqVO toolBuildStackReqVO);
}
