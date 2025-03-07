package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.QuerySuccGrayToolInfoResVO;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.defect.vo.TestTaskInfoVO;
import com.tencent.bk.codecc.defect.vo.ClocStatisticInfoVO;
import com.tencent.bk.codecc.defect.vo.ClocStatisticReqVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Api(tags = {"SERVICE_TEST_TASK"}, description = "测试任务管理接口")
@Path("/service/defect/test/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceTestTaskInfoRestResource {
    @ApiOperation("查询 TaskLogOverview")
    @Path("/get/taskLogOverview/{taskId}/{buildId}")
    @GET
    Result<TaskLogOverviewVO> getTaskLogOverview(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
            Long taskId,
            @PathParam("buildId")
            String buildId
    );

    @ApiOperation("查询该次扫描中的扫描成功的工具及其相应的开销信息")
    @Path("/query/grayTool/{taskId}/{buildId}")
    @POST
    Result<QuerySuccGrayToolInfoResVO> querySuccGrayToolInfo(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
            Long taskId,
            @PathParam("buildId")
            String buildId
    );

    @ApiOperation("查询指定任务的测试结果")
    @Path("/query/testTask/info/{toolName}/{taskId}/{buildId}")
    @GET
    Result<TestTaskInfoVO> queryTestTaskInfo(
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
            Long taskId,
            @PathParam("buildId")
            String buildId
    );

    @ApiOperation("批量查询特定任务特定语言的代码统计信息")
    @Path("/query/clocInfo")
    @POST
    Result<List<ClocStatisticInfoVO>> queryClocInfo(
            @ApiParam(value = "请求体", required = true)
            ClocStatisticReqVO request
    );

}
