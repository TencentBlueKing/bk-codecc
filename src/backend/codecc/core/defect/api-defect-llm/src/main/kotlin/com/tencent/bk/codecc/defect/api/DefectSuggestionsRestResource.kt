package com.tencent.bk.codecc.defect.api

import com.tencent.bk.codecc.defect.vo.DefectSuggestionEvaluateVO
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TICKET
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_TASK_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.codecc.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.messaging.handler.annotation.MessageMapping
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.HeaderParam
import javax.ws.rs.GET
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType

/**
 * llm大语言模型修复建议接口
 * @author jimxzcai
 * @version V1.0
 * @date 2023/11/16
 */
@Api(tags = ["SERVICE_LLM"], description = "llm大语言模型接口")
@Path("/user/warn")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface DefectSuggestionsRestResource {

    @ApiOperation("修复建议内容")
    @MessageMapping("/defect/suggestion")
    @Path("/defect/suggestion")
    @POST
    fun defectSuggestion(
        @ApiParam(value = "bkTicket", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TICKET)
        bkTicket: String?,

        @ApiParam(value = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String?,

        @ApiParam(value = "任务ID", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
        taskId: String?,

        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,

        @ApiParam(value = "获取文件片段相关参数", required = true)
        request: CommonDefectDetailQueryReqVO
    ): Result<String?>

    @ApiOperation("保存修复建议评价")
    @Path("/defect/suggestion/evaluate/defectId/handle")
    @POST
    fun defectSuggestionEvaluateHandle(
        @ApiParam(value = "保存评价相关参数", required = true)
        request: DefectSuggestionEvaluateVO
    ): Result<Boolean?>

    @ApiOperation("获取修复建议评价")
    @Path("/defect/suggestion/evaluate/defectId/{defectId}")
    @GET
    fun defectSuggestionEvaluateList(
        @ApiParam(value = "获取评价相关参数", required = true)
        @PathParam("defectId")
        defectId: String
    ): Result<DefectSuggestionEvaluateVO>
}
