package com.tencent.bk.codecc.task.api;

import com.tencent.devops.common.api.BaseDataVO;
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
 * 基础数据接口类
 *
 * @version V1.0
 * @date 2021/8/17
 */
@Api(tags = {"BASE"}, description = "基础数据查询")
@Path("/user/base")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserBaseDataRestResource {

    @ApiOperation("根据参数代码获取数据")
    @Path("/paramCode/{paramCode}")
    @GET
    Result<BaseDataVO> getBaseDataByCode(
            @ApiParam(value = "参数代码", required = true)
            @PathParam(value = "paramCode")
                    String paramCode);

}
