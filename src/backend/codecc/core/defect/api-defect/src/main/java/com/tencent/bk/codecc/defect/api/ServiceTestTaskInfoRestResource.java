package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.QuerySuccGrayToolInfoResVO;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.defect.vo.TestTaskInfoVO;
import com.tencent.bk.codecc.defect.vo.ClocStatisticInfoVO;
import com.tencent.bk.codecc.defect.vo.ClocStatisticReqVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Tag(name = "SERVICE_TEST_TASK", description = "测试任务管理接口")
@Path("/service/defect/test/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceTestTaskInfoRestResource {
    @Operation(summary = "查询 TaskLogOverview")
    @Path("/get/taskLogOverview/{taskId}/{buildId}")
    @GET
    Result<TaskLogOverviewVO> getTaskLogOverview(
            @Parameter(description = "任务ID", required = true)
            @PathParam("taskId")
            Long taskId,
            @PathParam("buildId")
            String buildId
    );

    @Operation(summary = "查询该次扫描中的扫描成功的工具及其相应的开销信息")
    @Path("/query/grayTool/{taskId}/{buildId}")
    @POST
    Result<QuerySuccGrayToolInfoResVO> querySuccGrayToolInfo(
            @Parameter(description = "任务ID", required = true)
            @PathParam("taskId")
            Long taskId,
            @PathParam("buildId")
            String buildId
    );

    @Operation(summary = "查询指定任务的测试结果")
    @Path("/query/testTask/info/{toolName}/{taskId}/{buildId}")
    @GET
    Result<TestTaskInfoVO> queryTestTaskInfo(
            @Parameter(description = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @Parameter(description = "任务ID", required = true)
            @PathParam("taskId")
            Long taskId,
            @PathParam("buildId")
            String buildId
    );

    @Operation(summary = "批量查询特定任务特定语言的代码统计信息")
    @Path("/query/clocInfo")
    @POST
    Result<List<ClocStatisticInfoVO>> queryClocInfo(
            @Parameter(description = "请求体", required = true)
            ClocStatisticReqVO request
    );

}
