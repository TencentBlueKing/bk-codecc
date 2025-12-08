package com.tencent.bk.codecc.defect.api.report;

import com.tencent.bk.codecc.defect.vo.common.SnapShotVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * 快照构建信息接口
 */
@Tag(name = "BUILD_SNAPSHOT", description = "快照构建信息接口")
@Path("/build/snapshot")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildSnapShotResource {
    @Operation(summary = "获取某个任务的构建快照")
    @Path("/project/{projectId}/tasks/{taskId}/get")
    @GET
    Result<SnapShotVO> get(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
            String projectId,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
            Long taskId,
        @Parameter(description = "构建id", required = true)
        @QueryParam(value = "buildId")
            String buildId);

    @Operation(summary = "获取某次构建快照的红线数据上传状态")
    @Path("/projectId/{projectId}/taskId/{taskId}/buildId/{buildId}/metadataReportStatus")
    @GET
    Result<Boolean> getMetadataReportStatus(
        @Parameter(description = "项目ID", required = true)
        @PathParam(("projectId"))
            String projectId,
        @Parameter(description = "任务ID", required = true)
        @PathParam(("taskId"))
            Long taskId,
        @Parameter(description = "构建ID", required = true)
        @PathParam(("buildId"))
                String buildId);
}
