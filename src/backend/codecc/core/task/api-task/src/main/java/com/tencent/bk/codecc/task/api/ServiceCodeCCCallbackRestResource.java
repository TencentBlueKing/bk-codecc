package com.tencent.bk.codecc.task.api;

import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.CodeCCCallbackEvent;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "SERVICE_CODECC_CALLBACK", description = "CODECC 回调接口")
@Path("/service/codecc/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceCodeCCCallbackRestResource {

    @Operation(summary = "根据任务ID查询注册的回调事件")
    @Path("/task/events")
    @GET
    Result<List<CodeCCCallbackEvent>> getTaskEvents(
            @Parameter(description = "任务ID")
            @QueryParam("taskId")
            Long taskId
    );
}
