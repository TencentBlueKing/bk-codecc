package com.tencent.bk.codecc.defect.api

import com.tencent.bk.codecc.task.vo.GrayTaskStatVO
import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.pojo.codecc.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_CLUSTER_STATISTIC", description = "聚类统计接口")
@Path("/service/statistic")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface(value = "defect")
interface ServiceStatisticRestResource {

    @Operation(summary = "获取lint类工具统计信息")
    @Path("/taskId/{taskId}/toolName/{toolName}/buildId/{buildId}")
    @GET
    fun getLintStatInfo(
        @Parameter(description = "任务id", required = true)
        @PathParam("taskId")
        taskId: Long,
        @Parameter(description = "工具名", required = true)
        @PathParam("toolName")
        toolName: String,
        @Parameter(description = "构建id", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<GrayTaskStatVO?>
}
