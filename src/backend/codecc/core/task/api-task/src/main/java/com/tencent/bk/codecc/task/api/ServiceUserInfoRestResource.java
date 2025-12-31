package com.tencent.bk.codecc.task.api;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.devops.common.api.OrgInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;


@Tag(name = "USER", description = "用户信息查询")
@Path("/service/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceUserInfoRestResource {
    @Operation(summary = "获取用户组织架构信息")
    @Path("/getOrgInfo")
    @GET
    Result<OrgInfoVO> getOrgInfo(
            @Parameter(description = "用户id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId
    );

    @Operation(summary = "获取用户直属Leader信息")
    @Path("/getUserDirectLeader")
    @GET
    Result<String> getUserDirectLeader(
            @Parameter(description = "用户id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId
    );

}
