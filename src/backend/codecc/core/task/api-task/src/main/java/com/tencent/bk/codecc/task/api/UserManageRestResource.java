package com.tencent.bk.codecc.task.api;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_ACCESS_TOKEN;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.task.vo.DevopsProjectVO;
import com.tencent.devops.common.api.annotation.UserLogin;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * 登录的接口类
 *
 * @version V1.0
 * @date 2019/4/19
 */
@Tag(name = "USER", description = "用户管理")
@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserManageRestResource {
    @Operation(summary = "获取用户信息")
    @Path("/userInfo")
    @GET
    @UserLogin
    Result getInfo(
            @Parameter(description = "用户id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId
    );


    @Operation(summary = "获取用户项目列表")
    @Path("/projects")
    @GET
    Result<List<DevopsProjectVO>> getProjectList(
            @Parameter(description = "用户id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId,
            @Parameter(description = "PAAS_CC token", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
                    String accessToken);

        @Path("/isProjectManager")
    @GET
    Result<Boolean> isProjectManager(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );
}
