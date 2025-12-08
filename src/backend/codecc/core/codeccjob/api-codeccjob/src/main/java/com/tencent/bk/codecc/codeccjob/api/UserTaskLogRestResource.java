package com.tencent.bk.codecc.codeccjob.api;

import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.log.pojo.QueryLogs;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 页面端获取任务日志接口
 *
 * @version V1.0QueryLogs
 * @date 2021/6/21
 */
@Tag(name = "LOGS", description = "获取日志")
@Path("/user/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserTaskLogRestResource {



    @Operation(summary = "查询日志")
    @Path("/{projectId}/{pipelineId}/{buildId}")
    @GET
    Result<QueryLogs> getInitLogs(
            @Parameter(description = "项目ID", required = true)
            @PathParam(value = "projectId")
                    String projectId,
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @Parameter(description = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
                    String pipelineId,
            @Parameter(description = "构建ID", required = true)
            @PathParam(value = "buildId")
                    String buildId,
            @Parameter(description = "搜索关键字", required = false)
            @QueryParam("search")
                    String search
    );

    @Operation(summary = "查询日志")
    @Path("/{projectId}/{pipelineId}/{buildId}/after")
    @GET
    Result<QueryLogs> getAfterLogs(
            @Parameter(description = "项目ID", required = true)
            @PathParam(value = "projectId")
                    String projectId,
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @Parameter(description = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
                    String pipelineId,
            @Parameter(description = "构建ID", required = true)
            @PathParam(value = "buildId")
                    String buildId,
            @Parameter(description = "开始索引", required = false)
            @QueryParam("start")
                    Integer start,
            @Parameter(description = "搜索关键字", required = false)
            @QueryParam("search")
                    String search
    );
}