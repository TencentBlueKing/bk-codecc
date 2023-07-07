package com.tencent.bk.codecc.task.api;

import com.tencent.devops.common.api.DynamicConfigVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * 动态配置
 *
 */
@Api(tags = {"SERVICE_DYNAMIC_CONFIG"}, description = "动态配置服务接口")
@Path("/service/dynamic/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceDynamicConfigResource {


    @ApiOperation("查询构建ID关系")
    @Path("/key/{key}")
    @GET
    Result<DynamicConfigVO> getConfigByKey(@ApiParam(value = "键")
                                                     @PathParam("key")
                                                             String key);
}
