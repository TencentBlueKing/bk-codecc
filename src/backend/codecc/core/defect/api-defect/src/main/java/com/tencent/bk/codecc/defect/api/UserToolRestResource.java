package com.tencent.bk.codecc.defect.api;

import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;

/**
 * 用户工具接口
 *
 * @version V1.0
 * @date 2021/6/24
 */
@Tag(name = "USER_TOOL", description = "工具接口")
@Path("/user/tool")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserToolRestResource {
    @Operation(summary = "获取用户上次构建的工具")
    @Path("/pipeline/{pipelineId}/getLastAnalyzeTool")
    @GET
    Result<List<String>> getLastAnalyzeTool(
        @Parameter(description = "流水线id", required = true)
        @PathParam("pipelineId")
            String pipelineId,
        @Parameter(description = "单流水线多任务标识")
        @QueryParam("multiPipelineMark")
            String multiPipelineMark
    );
}
