package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.BatchTestResultVO;
import com.tencent.bk.codecc.task.vo.DevopsProjectVO;
import com.tencent.bk.codecc.task.vo.QueryTestReportReqVO;
import com.tencent.bk.codecc.task.vo.RecommendedThresholdVO;
import com.tencent.bk.codecc.task.vo.RepoScaleVO;
import com.tencent.bk.codecc.task.vo.StartRandomTestReqVO;
import com.tencent.bk.codecc.task.vo.StartTestReqVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskIdVO;
import com.tencent.bk.codecc.task.vo.TestTaskReportVO;
import com.tencent.bk.codecc.task.vo.ToolBasicInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 测试任务管理接口
 *
 * @version V1.0
 * @date 2024/3/19
 */
@Api(tags = {"USER_TEST_TASK"}, description = "测试任务管理接口")
@Path("/user/test/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserTestTaskRestResource {

    @GET
    @Path("/getLatestVersion/{toolName}")
    @ApiOperation("获取最新的已发布版本号")
    Result<String> getLatestVersion(
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName
    );

    @POST
    @Path("/")
    @ApiOperation("创建测试任务")
    Result<TaskIdVO> registerTestTask(
            @ApiParam(value = "任务信息", required = true)
            @Valid
            TaskDetailVO taskDetailVO,
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );

    @GET
    @Path("/repoScale/list")
    Result<List<RepoScaleVO>> getRepoScaleList();

    @GET
    @Path("/recommendedThreshold")
    Result<RecommendedThresholdVO> getRecommendedThreshold();

    @GET
    @Path("/project/list")
    @ApiOperation("获取用户有管理权限的项目id")
    Result<List<DevopsProjectVO>> getProjectIdsByUserName(
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId
    );

    @GET
    @Path("/info/toolName/{toolName}")
    @ApiOperation("获取工具的语言和所有规则集名称")
    Result<ToolBasicInfoVO> getBasicInfo(
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName
    );

    @GET
    @Path("/list/{toolName}/{projectId}")
    @ApiOperation("list 测试任务")
    Result<List<TaskBaseVO>> listTask(
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @ApiParam(value = "项目id", required = true)
            @PathParam(value = "projectId")
            String projectId
    );

    @GET
    @Path("/has/testTask/{projectId}")
    @ApiOperation("返回该项目中是否有测试任务")
    Result<Boolean> hasTestTask(
            @ApiParam(value = "项目id", required = true)
            @PathParam(value = "projectId")
            String projectId
    );

    @PUT
    @Path("/delete/testTask/{projectId}/{taskId}")
    @ApiOperation("指定测试删除任务")
    Result<Boolean> deleteTestTask(
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "项目id", required = true)
            @PathParam(value = "projectId")
            String projectId,
            @ApiParam(value = "任务id", required = true)
            @PathParam(value = "taskId")
            Long taskId
    );

    @POST
    @Path("/start/{toolName}")
    @ApiOperation("开始指定测试")
    Result<Boolean> start(
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @ApiParam(value = "请求视图", required = true)
            StartTestReqVO startTestReqVO
    );

    @POST
    @Path("/start/randomTest")
    @ApiOperation("开始随机测试")
    Result<Boolean> startRandomTest(
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "请求视图", required = true)
            StartRandomTestReqVO request
    );

    @GET
    @Path("/getTestStatus/{toolName}/{version}")
    @ApiOperation("获取指定测试的当前状态 (即指定测试是否完成)")
    Result<Boolean> getTestStatus(
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @ApiParam(value = "版本", required = true)
            @PathParam(value = "version")
            String version
    );

    @GET
    @Path("/getResult/{toolName}/{version}")
    @ApiOperation("获取测试结果")
    Result<BatchTestResultVO> getBatchTestResult(
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @ApiParam(value = "版本", required = true)
            @PathParam(value = "version")
            String version,
            @QueryParam(value = "stage")
            Integer stage
    );

    @GET
    @Path("/listVersion/{toolName}")
    @ApiOperation("获取工具的测试版本号列表")
    Result<List<String>> listVersion(
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @QueryParam(value = "stage")
            Integer stage
    );

    @POST
    @Path("/testReport/{toolName}/{stage}")
    Result<List<TestTaskReportVO>> listTestReport(
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @ApiParam(value = "阶段号", required = true)
            @PathParam(value = "stage")
            Integer stage,
            @ApiParam(value = "请求视图", required = true)
            QueryTestReportReqVO queryTestReportReqVO,
            @ApiParam(value = "第几页")
            @QueryParam("pageNum")
            Integer pageNum,
            @ApiParam(value = "每页多少条")
            @QueryParam("pageSize")
            Integer pageSize
    );

    @POST
    @Path("/testReport/count/{toolName}/{stage}")
    Result<Integer> countTestReport(
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @ApiParam(value = "阶段号", required = true)
            @PathParam(value = "stage")
            Integer stage,
            @ApiParam(value = "请求视图", required = true)
            QueryTestReportReqVO queryTestReportReqVO
    );
}
