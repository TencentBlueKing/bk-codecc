package com.tencent.bk.codecc.defect.api;

import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api(tags = {"BUILD_I18N"}, description = "国际化信息")
@Path("/build/i18n")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildI18NRestResource {

    @ApiOperation("获取用户国际化信息")
    @Path("/getLanguageTag")
    @GET
    Result<String> getLanguageTag(
            @ApiParam(value = "用户Id", required = true)
            @QueryParam("userId")
            String userId
    );
}
