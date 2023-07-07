package com.tencent.bk.codecc.openapi.v2

import com.tencent.bk.codecc.scanschedule.vo.ContentVO
import com.tencent.bk.codecc.scanschedule.vo.ScanResultVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.pojo.codecc.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

/**
 * 工具扫描接口，支持代码片段扫描
 *
 * @version V2.0
 * @date 2023/04/17
 */
@Api(tags = ["TOOL_SCAN"], description = "工具扫描接口，支持代码片段扫描")
@Path("/{apigw:apigw-user|apigw-app|apigw}/v2/scan")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwToolScanResourceV2 {

    @ApiOperation("工具扫描接口")
    @Path("/contentScan")
    @POST
    fun scan(
        @ApiParam(value = "应用code", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "片段扫描请求", required = true)
        scanContentVO: ContentVO?
    ): Result<ScanResultVO>
}