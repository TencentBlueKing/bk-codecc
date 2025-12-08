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
import com.tencent.bk.codecc.task.vo.TestTaskResultReqVO;
import com.tencent.bk.codecc.task.vo.TestTaskStatusReqVO;
import com.tencent.bk.codecc.task.vo.ToolBasicInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 测试任务管理接口
 *
 * @version V1.0
 * @date 2024/3/19
 */
@Tag(name = "USER_TEST_TASK", description = "测试任务管理接口")
@Path("/user/test/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserTestTaskRestResource {

    @GET
    @Path("/getLatestVersion/{toolName}")
    @Operation(summary = "获取最新的已发布版本号")
    Result<String> getLatestVersion(
            @Parameter(description = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName
    );

    @POST
    @Path("/")
    @Operation(summary = "创建测试任务")
    Result<TaskIdVO> registerTestTask(
            @Parameter(description = "任务信息", required = true)
            @Valid
            TaskDetailVO taskDetailVO,
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "当前用户", required = true)
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
    @Operation(summary = "获取用户有管理权限的项目id")
    Result<List<DevopsProjectVO>> getProjectIdsByUserName(
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId
    );

    @GET
    @Path("/info/toolName/{toolName}")
    @Operation(summary = "获取工具的语言和所有规则集名称")
    Result<ToolBasicInfoVO> getBasicInfo(
            @Parameter(description = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName
    );

    @GET
    @Path("/list/{toolName}/{projectId}")
    @Operation(summary = "list 测试任务")
    Result<List<TaskBaseVO>> listTask(
            @Parameter(description = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @Parameter(description = "项目id", required = true)
            @PathParam(value = "projectId")
            String projectId
    );

    @GET
    @Path("/has/testTask/{projectId}")
    @Operation(summary = "返回该项目中是否有测试任务")
    Result<Boolean> hasTestTask(
            @Parameter(description = "项目id", required = true)
            @PathParam(value = "projectId")
            String projectId
    );

    @PUT
    @Path("/delete/testTask/{projectId}/{taskId}")
    @Operation(summary = "指定测试删除任务")
    Result<Boolean> deleteTestTask(
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @Parameter(description = "项目id", required = true)
            @PathParam(value = "projectId")
            String projectId,
            @Parameter(description = "任务id", required = true)
            @PathParam(value = "taskId")
            Long taskId
    );

    @POST
    @Path("/start/{toolName}")
    @Operation(summary = "开始指定测试")
    Result<Boolean> start(
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @Parameter(description = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @Parameter(description = "请求视图", required = true)
            StartTestReqVO startTestReqVO
    );

    @POST
    @Path("/start/randomTest")
    @Operation(summary = "开始随机测试")
    Result<Boolean> startRandomTest(
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "请求视图", required = true)
            StartRandomTestReqVO request
    );

    @POST
    @Path("/getTestStatus")
    @Operation(summary = "获取指定测试的当前状态 (即指定测试是否完成)")
    Result<Boolean> getTestStatus(
            TestTaskStatusReqVO request
    );

    @POST
    @Path("/getResult")
    @Operation(summary = "获取测试结果")
    Result<BatchTestResultVO> getBatchTestResult(
            TestTaskResultReqVO request
    );

    @GET
    @Path("/listVersion/{toolName}")
    @Operation(summary = "获取工具的测试版本号列表")
    Result<List<String>> listVersion(
            @Parameter(description = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @QueryParam(value = "stage")
            Integer stage
    );

    @POST
    @Path("/testReport/{toolName}/{stage}")
    Result<List<TestTaskReportVO>> listTestReport(
            @Parameter(description = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @Parameter(description = "阶段号", required = true)
            @PathParam(value = "stage")
            Integer stage,
            @Parameter(description = "请求视图", required = true)
            QueryTestReportReqVO queryTestReportReqVO,
            @Parameter(description = "第几页")
            @QueryParam("pageNum")
            Integer pageNum,
            @Parameter(description = "每页多少条")
            @QueryParam("pageSize")
            Integer pageSize
    );

    @POST
    @Path("/testReport/count/{toolName}/{stage}")
    Result<Integer> countTestReport(
            @Parameter(description = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @Parameter(description = "阶段号", required = true)
            @PathParam(value = "stage")
            Integer stage,
            @Parameter(description = "请求视图", required = true)
            QueryTestReportReqVO queryTestReportReqVO
    );

}
