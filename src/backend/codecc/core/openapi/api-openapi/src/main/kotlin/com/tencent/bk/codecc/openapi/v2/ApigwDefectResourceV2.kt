package com.tencent.bk.codecc.openapi.v2

import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqOldVO
import com.tencent.bk.codecc.defect.vo.ToolClocRspVO
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskVO
import com.tencent.bk.codecc.task.vo.tianyi.QueryMyTasksReqVO
import com.tencent.bk.codecc.task.vo.tianyi.TaskInfoVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.codecc.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_V2_DEFECT"], description = "OPEN-API-V2-告警查询")
@Path("/{apigw:apigw-user|apigw-app|apigw}/v2/defect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwDefectResourceV2 {

    @ApiOperation("根据作者获取对应任务信息列表")
    @Path("/myTasks")
    @POST
    fun getTasksByAuthor(
        @ApiParam(value = "请求对象模型", required = true)
        reqVO: QueryMyTasksReqVO
    ): Result<Page<TaskInfoVO>>

    @ApiOperation("查询代码行数情况")
    @Path("/codeLine/taskId/{taskId}")
    @GET
    fun queryCodeLineInfo(
        @ApiParam(value = "任务ID", required = true)
        @PathParam(value = "taskId")
        taskId: Long,
        @ApiParam(value = "工具名称", required = false)
        @QueryParam(value = "toolName")
        @DefaultValue("SCC")
        toolName: String
    ): Result<ToolClocRspVO>


    @ApiOperation("通过流水线ID获取任务信息")
    @Path("/pipelines/{pipelineId}")
    @GET
    @Deprecated("即将废弃")
    fun getPipelineTask(
        @ApiParam(value = "流水线ID", required = true)
        @PathParam(value = "pipelineId")
        pipelineId: String,
        @ApiParam(value = "单流水线对应多任务标识", required = false)
        @QueryParam(value = "multiPipelineMark")
        multiPipelineMark: String?,
        @ApiParam(value = "当前用户")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        user: String? = null
    ): Result<PipelineTaskVO>


    @ApiOperation("通过流水线ID获取任务信息")
    @Path("/pipelines/{pipelineId}/allTask")
    @GET
    @Deprecated("即将废弃")
    fun getPipelineAllTask(
        @ApiParam(value = "流水线ID", required = true)
        @PathParam(value = "pipelineId")
        pipelineId: String,
        @ApiParam(value = "当前用户")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        user: String? = null
    ): Result<List<PipelineTaskVO>>

    @ApiOperation("作者转换")
    @Path("/author/taskId/{taskId}/projectId/{projectId}")
    @PUT
    fun authorTransfer(
        @ApiParam(value = "api类型", required = true)
        @PathParam(value = "apigw")
        apigw: String,
        @ApiParam(value = "任务id", required = true)
        @PathParam(value = "taskId")
        taskId: Long,
        @ApiParam(value = "项目id", required = true)
        @PathParam(value = "projectId")
        projectId: String,
        @ApiParam(value = "appCode", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String,
        @ApiParam(value = "transferAuthorPairs", required = false)
        batchDefectProcessReqOldVO: BatchDefectProcessReqOldVO,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<Boolean>

}
