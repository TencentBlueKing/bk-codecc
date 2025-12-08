package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.GrayTaskStatVO;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * 灰度项目接口
 *
 * @version V1.0
 * @date 2021/1/05
 */
@Tag(name = "SERVICE_RELATIONSHIP")
@Path("/service/gray")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceGrayToolProjectResource {
//    @Operation(summary = "查询构建ID关系")
//    @Path("/project/{projectId}")
//    @GET
//    Result<GrayToolProjectVO> getGrayToolProjectInfoByProjectId(
//            @Parameter(description = "项目ID")
//            @PathParam("projectId")
//                    String projectId);

    @Operation(summary = "返回项目下所有工具的灰度配置信息")
    @Path("/project/{projectId}/detail")
    @GET
    Result<List<GrayToolProjectVO>> getGrayToolProjectDetail(
            @Parameter(description = "项目ID")
            @PathParam("projectId")
                    String projectId);

    @Operation(summary = "批量查询灰度项目")
    @Path("/project/list")
    @POST
    Result<List<GrayToolProjectVO>> getGrayToolProjectByProjectIds(Set<String> projectIdSet);

    @Operation(summary = "查询构建ID关系")
    @Path("/report/taskId/{taskId}/buildId/{buildId}")
    @PUT
    Result<Boolean> processGrayReport(
            @Parameter(description = "任务id")
            @PathParam("taskId")
            Long taskId,
            @Parameter(description = "构建id")
            @PathParam("buildId")
            String buildId,
            @Parameter(description = "工具名")
            @QueryParam("toolName")
            String toolName,
            @Parameter(description = "告警数量")
            GrayTaskStatVO grayTaskStatVO);
}
