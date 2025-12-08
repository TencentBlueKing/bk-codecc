package com.tencent.bk.codecc.defect.api

import com.tencent.bk.codecc.defect.vo.DefectSuggestionEvaluateVO
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TICKET
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_TASK_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.codecc.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.messaging.handler.annotation.MessageMapping
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.GET
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.MediaType

/**
 * llm大语言模型修复建议接口
 * @author jimxzcai
 * @version V1.0
 * @date 2023/11/16
 */
@Tag(name = "SERVICE_LLM", description = "llm大语言模型接口")
@Path("/user/warn")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface DefectSuggestionsRestResource {

    @Operation(summary = "修复建议内容")
    @MessageMapping("/defect/suggestion")
    @Path("/defect/suggestion")
    @POST
    fun defectSuggestion(
        @Parameter(description = "bkTicket", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TICKET)
        bkTicket: String?,

        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String?,

        @Parameter(description = "任务ID", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
        taskId: String?,

        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,

        @Parameter(description = "获取文件片段相关参数", required = true)
        request: CommonDefectDetailQueryReqVO
    ): Result<String?>

    @Operation(summary = "保存修复建议评价")
    @Path("/defect/suggestion/evaluate/defectId/handle")
    @POST
    fun defectSuggestionEvaluateHandle(
        @Parameter(description = "保存评价相关参数", required = true)
        request: DefectSuggestionEvaluateVO
    ): Result<Boolean?>

    @Operation(summary = "获取修复建议评价")
    @Path("/defect/suggestion/evaluate/defectId/{defectId}")
    @GET
    fun defectSuggestionEvaluateList(
        @Parameter(description = "获取评价相关参数", required = true)
        @PathParam("defectId")
        defectId: String
    ): Result<DefectSuggestionEvaluateVO>
}
