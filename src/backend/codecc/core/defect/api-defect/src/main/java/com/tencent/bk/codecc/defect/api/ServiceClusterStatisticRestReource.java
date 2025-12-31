package com.tencent.bk.codecc.defect.api;

import com.tencent.devops.common.api.annotation.ServiceInterface;
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "SERVICE_CLUSTER_STATISTIC", description = "聚类统计接口")
@Path("/service/clusterStatistic")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface(value = "defect")
public interface ServiceClusterStatisticRestReource {
    @Operation(summary = "获取聚类统计信息")
    @Path("/")
    @GET
    Result<List<BaseClusterResultVO>> getClusterStatistic(
            @Parameter(description = "任务id", required = true)
            @QueryParam("taskId")
                    long taskId,
            @Parameter(description = "构建号", required = true)
            @QueryParam("buildId")
                    String buildId
    );
}
