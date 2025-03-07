package com.tencent.bk.codecc.task.api;

import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.CodeCCCallbackEvent;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api(tags = {"SERVICE_CODECC_CALLBACK"}, description = "CODECC 回调接口")
@Path("/service/codecc/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceCodeCCCallbackRestResource {

    @ApiOperation("根据任务ID查询注册的回调事件")
    @Path("/task/events")
    @GET
    Result<List<CodeCCCallbackEvent>> getTaskEvents(
            @ApiParam(value = "任务ID")
            @QueryParam("taskId")
            Long taskId
    );
}
