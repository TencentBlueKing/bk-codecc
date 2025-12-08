package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.MetricsVO;
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

@Tag(name = "SERVICE_DEFECT", description = "告警模块树服务")
@Path("/service/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceMetricsRestResource {
    @Operation(summary = "度量信息")
    @Path("/")
    @GET
    Result<MetricsVO> getMetrics(
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(value = "taskId")
                    Long taskId,
            @Parameter(description = "构建号", required = true)
            @QueryParam(value = "buildId")
                    String buildId);

    @Operation(summary = "度量信息")
    @Path("/list")
    @POST
    Result<List<MetricsVO>> getMetrics(
            @Parameter(description = "任务ID", required = true)
                    List<Long> taskIds);

    @Operation(summary = "根据task id获取最新的度量信息")
    @Path("/taskId/{taskId}")
    @GET
    Result<MetricsVO> getLatestMetrics(
        @Parameter(description = "任务ID", required = true)
        @PathParam(value = "taskId")
        Long taskId
    );
}
