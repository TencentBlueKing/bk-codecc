package com.tencent.bk.codecc.defect.api.report;

import com.tencent.bk.codecc.defect.vo.common.SnapShotVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * 快照构建信息接口
 */
@Api(tags = {"BUILD_SNAPSHOT"}, description = "快照构建信息接口")
@Path("/build/snapshot")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildSnapShotResource {
    @ApiOperation("获取某个任务的构建快照")
    @Path("/project/{projectId}/tasks/{taskId}/get")
    @GET
    Result<SnapShotVO> get(
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
            String projectId,
        @ApiParam(value = "任务ID", required = true)
        @PathParam("taskId")
            Long taskId,
        @ApiParam(value = "构建id", required = true)
        @QueryParam(value = "buildId")
            String buildId);

    @ApiOperation("获取某次构建快照的红线数据上传状态")
    @Path("/projectId/{projectId}/taskId/{taskId}/buildId/{buildId}/metadataReportStatus")
    @GET
    Result<Boolean> getMetadataReportStatus(
        @ApiParam(value = "项目ID", required = true)
        @PathParam(("projectId"))
            String projectId,
        @ApiParam(value = "任务ID", required = true)
        @PathParam(("taskId"))
            Long taskId,
        @ApiParam(value = "构建ID", required = true)
        @PathParam(("buildId"))
                String buildId);
}
