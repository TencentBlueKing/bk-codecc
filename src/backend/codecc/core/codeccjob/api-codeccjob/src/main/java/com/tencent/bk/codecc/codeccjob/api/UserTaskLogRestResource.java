package com.tencent.bk.codecc.codeccjob.api;

import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.log.pojo.QueryLogs;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 页面端获取任务日志接口
 *
 * @version V1.0QueryLogs
 * @date 2021/6/21
 */
@Api(tags = {"LOGS"}, description = "获取日志")
@Path("/user/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserTaskLogRestResource {



    @ApiOperation("查询日志")
    @Path("/{projectId}/{pipelineId}/{buildId}")
    @GET
    Result<QueryLogs> getInitLogs(
            @ApiParam(value = "项目ID", required = true)
            @PathParam(value = "projectId")
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
                    String pipelineId,
            @ApiParam(value = "构建ID", required = true)
            @PathParam(value = "buildId")
                    String buildId,
            @ApiParam(value = "搜索关键字", required = false)
            @QueryParam("search")
                    String search
    );

    @ApiOperation("查询日志")
    @Path("/{projectId}/{pipelineId}/{buildId}/after")
    @GET
    Result<QueryLogs> getAfterLogs(
            @ApiParam(value = "项目ID", required = true)
            @PathParam(value = "projectId")
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
                    String pipelineId,
            @ApiParam(value = "构建ID", required = true)
            @PathParam(value = "buildId")
                    String buildId,
            @ApiParam(value = "开始索引", required = false)
            @QueryParam("start")
                    Integer start,
            @ApiParam(value = "搜索关键字", required = false)
            @QueryParam("search")
                    String search
    );
}