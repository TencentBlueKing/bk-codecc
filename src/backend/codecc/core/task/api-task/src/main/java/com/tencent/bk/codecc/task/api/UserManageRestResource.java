package com.tencent.bk.codecc.task.api;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_ACCESS_TOKEN;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.task.vo.DevopsProjectVO;
import com.tencent.devops.common.api.annotation.UserLogin;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * 登录的接口类
 *
 * @version V1.0
 * @date 2019/4/19
 */
@Api(tags = {"USER"}, description = "用户管理")
@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserManageRestResource {
    @ApiOperation("获取用户信息")
    @Path("/userInfo")
    @GET
    @UserLogin
    Result getInfo(
            @ApiParam(value = "用户id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId
    );


    @ApiOperation("获取用户项目列表")
    @Path("/projects")
    @GET
    Result<List<DevopsProjectVO>> getProjectList(
            @ApiParam(value = "用户id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId,
            @ApiParam(value = "PAAS_CC token", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
                    String accessToken);

    @ApiOperation("")
    @Path("/isProjectManager")
    @GET
    Result<Boolean> isProjectManager(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );
}
