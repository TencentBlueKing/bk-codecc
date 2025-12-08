package com.tencent.bk.codecc.quartz.api

import com.tencent.bk.codecc.quartz.pojo.JobInfoVO
import com.tencent.bk.codecc.quartz.pojo.ShardingResultVO
import com.tencent.devops.common.api.pojo.codecc.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

/**
 * 获取任务详情接口
 */
@Tag(name = "USER_JOB", description = "任务管理接口")
@Path("/user/job")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserJobRestResource {

    @Operation(summary = "获取正在执行job信息")
    @Path("/existing")
    @GET
    fun getExistingJob(): Result<List<JobInfoVO>>

    @Operation(summary = "删除所有正在执行的任务")
    @Path("/all/jobs/{dataDelete}")
    @DELETE
    fun deleteAllJobs(
        @Parameter(description = "是否删除表中数据")
        @PathParam("dataDelete")
        dataDelete : Int
    ) : Result<Boolean>


    @Operation(summary = "初始化任务信息")
    @Path("/all/jobs")
    @POST
    fun initAllJobs() : Result<Boolean>

    @Operation(summary = "刷新所有开源扫描的cron表达式")
    @Path("/openSource/cron/period/{period}/startTime/{startTime}")
    @PUT
    fun refreshOpenSourceCronExpression(
        @Parameter(description = "开源扫描时间周期")
        @PathParam("period")
        period : Int,
        @Parameter(description = "开源扫描时间起点")
        @PathParam("startTime")
        startTime : Int) : Result<Boolean>

    @Operation(summary = "获取分片信息")
    @Path("/shardingResult")
    @GET
    fun getShardingResult(): Result<ShardingResultVO?>

    @Path("/className/{className}")
    @GET
    fun getJobList(
        @Parameter(description = "业务类")
        @PathParam("className")
        className: String
    ): Result<List<JobInfoVO>>
}