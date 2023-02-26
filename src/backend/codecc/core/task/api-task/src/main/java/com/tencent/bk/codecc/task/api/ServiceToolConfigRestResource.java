package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Api(tags = {"SERVICE_TOOLCONFIG"}, description = "任务-工具映射管理接口")
@Path("/service/toolConfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceToolConfigRestResource {
    @ApiOperation("获取任务总数")
    @Path("/")
    @GET
    Result<List<ToolConfigInfoVO>> getTaskIdByPage(
            @ApiParam(value = "第几页")
            @QueryParam("taskId")
              Long taskId
    );
}
