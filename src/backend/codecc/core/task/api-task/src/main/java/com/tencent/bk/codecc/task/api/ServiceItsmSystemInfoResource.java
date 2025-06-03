package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.itsm.ItsmSystemInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * 获取ITSM系统信息
 */
@Api(tags = {"SERVICE_ITSM_INFO"}, description = "获取ITSM系统信息")
@Path("/service/itsm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceItsmSystemInfoResource {

    @ApiOperation("查询构建ID关系")
    @Path("/system/{system}")
    @GET
    Result<ItsmSystemInfoVO> getSystemInfo(
            @ApiParam(value = "系统")
            @PathParam("system")
            String system,
            @ApiParam(value = "版本")
            @QueryParam("version")
            Integer version
    );
}
