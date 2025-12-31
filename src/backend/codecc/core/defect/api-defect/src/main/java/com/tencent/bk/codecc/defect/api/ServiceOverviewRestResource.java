package com.tencent.bk.codecc.defect.api;

import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "SERVICE_STATISTIC", description = "服务相关统计数据接口")
@Path("/service/statistic")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceOverviewRestResource {

    @Operation(summary = "获取用户个人待处理数据")
    @Path("/overview/refresh")
    @POST
    Result<Boolean> refresh(
        @Parameter(description = "任务id")
        @QueryParam("taskId")
            Long taskId,
        @Parameter(description = "额外信息")
        @QueryParam("extraInfo")
            String extraInfo);
}
