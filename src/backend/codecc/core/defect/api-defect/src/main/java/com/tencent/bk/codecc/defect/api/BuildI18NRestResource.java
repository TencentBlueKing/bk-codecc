package com.tencent.bk.codecc.defect.api;

import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "BUILD_I18N", description = "国际化信息")
@Path("/build/i18n")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildI18NRestResource {

    @Operation(summary = "获取用户国际化信息")
    @Path("/getLanguageTag")
    @GET
    Result<String> getLanguageTag(
            @Parameter(description = "用户Id", required = true)
            @QueryParam("userId")
            String userId
    );
}
