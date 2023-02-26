package com.tencent.bk.codecc.defect.api;

import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.RequestParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;

/**
 * 用户工具接口
 *
 * @version V1.0
 * @date 2021/6/24
 */
@Api(tags = {"USER_TOOL"}, description = "工具接口")
@Path("/user/tool")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserToolRestResource {
    @ApiOperation("获取用户上次构建的工具")
    @Path("/pipeline/{pipelineId}/getLastAnalyzeTool")
    @GET
    Result<List<String>> getLastAnalyzeTool(
        @ApiParam(value = "流水线id", required = true)
        @PathParam("pipelineId")
            String pipelineId,
        @ApiParam(value = "单流水线多任务标识")
        @QueryParam("multiPipelineMark")
            String multiPipelineMark
    );
}
