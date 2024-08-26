package com.tencent.bk.codecc.openapi.v2

import com.tencent.bk.codecc.openapi.v2.vo.ProjectCallbackEvent
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.devops.common.api.pojo.codecc.Result
import com.tencent.devops.common.web.security.AuthCodeCCToken
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType


@Api(tags = ["OPEN_API_V2_CALLBACK"], description = "OPEN-API-V2-回调接口")
@AuthCodeCCToken
@Path("/open/v2/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpenApiCallbackResource {

    @POST
    @Path("/instances/list")
    @ApiOperation("特定资源列表")
    fun resourceList(
        @ApiParam(value = "回调信息", required = true)
        callBackInfo: CallbackRequestDTO
    ): String

    @POST
    @Path("/project/event")
    @ApiOperation("项目时间回调")
    fun projectEvent(
        @ApiParam(value = "回调信息", required = true)
        callBackInfo: ProjectCallbackEvent
    ): Result<String>
}
