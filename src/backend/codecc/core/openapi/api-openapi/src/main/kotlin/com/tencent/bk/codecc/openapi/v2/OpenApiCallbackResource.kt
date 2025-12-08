package com.tencent.bk.codecc.openapi.v2

import com.tencent.bk.codecc.openapi.v2.vo.ProjectCallbackEvent
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.devops.common.api.pojo.codecc.Result
import com.tencent.devops.common.web.security.AuthCodeCCToken
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType


@Tag(name = "OPEN_API_V2_CALLBACK", description = "OPEN-API-V2-回调接口")
@AuthCodeCCToken
@Path("/open/v2/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpenApiCallbackResource {

    @POST
    @Path("/instances/list")
    @Operation(summary = "特定资源列表")
    fun resourceList(
        @Parameter(description = "回调信息", required = true)
        callBackInfo: CallbackRequestDTO
    ): String

    @POST
    @Path("/project/event")
    @Operation(summary = "项目时间回调")
    fun projectEvent(
        @Parameter(description = "回调信息", required = true)
        callBackInfo: ProjectCallbackEvent
    ): Result<String>
}
