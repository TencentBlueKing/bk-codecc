package com.tencent.bk.codecc.task.api;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.devops.common.api.OrgInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Api(tags = {"USER"}, description = "用户信息查询")
@Path("/service/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceUserInfoRestResource {
    @ApiOperation("获取用户组织架构信息")
    @Path("/getOrgInfo")
    @GET
    Result<OrgInfoVO> getOrgInfo(
            @ApiParam(value = "用户id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId
    );

    @ApiOperation("获取用户直属Leader信息")
    @Path("/getUserDirectLeader")
    @GET
    Result<String> getUserDirectLeader(
            @ApiParam(value = "用户id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId
    );

}
