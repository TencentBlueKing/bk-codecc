package com.tencent.bk.codecc.defect.api;

import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 工具管理页面权限相关接口
 */
@Api(tags = {"USER_TOOL_DEVELOPER"}, description = "工具开发者信息接口")
@Path("/user/tool/developer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserToolDeveloperInfoResource {

    @ApiOperation("将特定用户添加为特定工具的特定权限角色")
    @Path("/toolName/{toolName}/userName/{userName}")
    @POST
    Result<Boolean> addUserAsRole(
            @ApiParam(value = "调用接口的用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @ApiParam(value = "目的用户名", required = true)
            @PathParam("userName")
            String userName,
            @ApiParam(value = "角色类型")
            @QueryParam("role")
            Integer role
    );
}
