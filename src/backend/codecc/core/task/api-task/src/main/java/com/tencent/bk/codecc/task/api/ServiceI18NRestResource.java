package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.I18NMessageRequest;
import com.tencent.bk.codecc.task.vo.I18NMessageResponse;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(tags = {"SERVICE_TASK"}, description = "国际化")
@Path("/service/i18n")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceI18NRestResource {
    @ApiOperation("获取国际化信息")
    @Path("/getI18NMessage")
    @POST
    Result<I18NMessageResponse> getI18NMessage(I18NMessageRequest request);
}
