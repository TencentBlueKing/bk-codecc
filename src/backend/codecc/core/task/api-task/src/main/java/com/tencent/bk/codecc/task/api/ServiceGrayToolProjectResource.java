package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.GrayTaskStatVO;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * 灰度项目接口
 *
 * @version V1.0
 * @date 2021/1/05
 */
@Api(tags = {"SERVICE_RELATIONSHIP"})
@Path("/service/gray")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceGrayToolProjectResource {
//    @ApiOperation("查询构建ID关系")
//    @Path("/project/{projectId}")
//    @GET
//    Result<GrayToolProjectVO> getGrayToolProjectInfoByProjectId(
//            @ApiParam(value = "项目ID")
//            @PathParam("projectId")
//                    String projectId);

    @ApiOperation("返回项目下所有工具的灰度配置信息")
    @Path("/project/{projectId}/detail")
    @GET
    Result<List<GrayToolProjectVO>> getGrayToolProjectDetail(
            @ApiParam(value = "项目ID")
            @PathParam("projectId")
                    String projectId);

    @ApiOperation("批量查询灰度项目")
    @Path("/project/list")
    @POST
    Result<List<GrayToolProjectVO>> getGrayToolProjectByProjectIds(Set<String> projectIdSet);

    @ApiOperation("查询构建ID关系")
    @Path("/report/taskId/{taskId}/buildId/{buildId}")
    @PUT
    Result<Boolean> processGrayReport(
            @ApiParam(value = "任务id")
            @PathParam("taskId")
            Long taskId,
            @ApiParam(value = "构建id")
            @PathParam("buildId")
            String buildId,
            @ApiParam(value = "工具名")
            @QueryParam("toolName")
            String toolName,
            @ApiParam(value = "告警数量")
            GrayTaskStatVO grayTaskStatVO);
}
