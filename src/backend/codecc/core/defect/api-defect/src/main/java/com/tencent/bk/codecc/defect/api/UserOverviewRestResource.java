package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.TaskPersonalStatisticwVO;
import com.tencent.bk.codecc.defect.vo.toolintegration.DailyStatOverviewVO;
import com.tencent.bk.codecc.defect.vo.toolintegration.DailyTrendChartVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

@Tag(name = "USER_STATISTIC", description = "用户相关统计数据接口")
@Path("/user/statistic")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserOverviewRestResource {

    @Operation(summary = "获取用户个人待处理数据")
    @Path("/overview/personal")
    @GET
    Result<TaskPersonalStatisticwVO> overview(
        @Parameter(description = "任务id")
        @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
        @Parameter(description = "用户名")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String username);

    @Operation(summary = "获取用户个人待处理数据")
    @Path("/overview/personal/list")
    @GET
    Result<List<TaskPersonalStatisticwVO>> overviewList(
            @Parameter(description = "任务id")
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId);

    @Operation(summary = "获取用户个人待处理数据")
    @Path("/overview/personal/task/list")
    @POST
    Result<List<TaskPersonalStatisticwVO>> overviewByTaskList(
            @Parameter(description = "任务id列表")
                    List<Long> taskIdList,
            @Parameter(description = "用户名")
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String username);
}
