package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.I18NMessageRequest;
import com.tencent.bk.codecc.task.vo.I18NMessageResponse;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "SERVICE_TASK", description = "国际化")
@Path("/service/i18n")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceI18NRestResource {
    @Operation(summary = "获取国际化信息")
    @Path("/getI18NMessage")
    @POST
    Result<I18NMessageResponse> getI18NMessage(I18NMessageRequest request);
}
