package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.ToolMemberInfoVO;
import com.tencent.bk.codecc.defect.vo.developer.ToolDeveloperInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * 工具开发者信息相关的 build 接口
 */
@Tag(name = "BUILD_TOOL_DEVELOPER", description = "工具开发者信息相关的 build 接口")
@Path("/build/tool/developer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildToolDeveloperInfoResource {

    @Operation(summary = "获取工具的开发者权限信息")
    @Path("/getPermissionInfo/{toolName}")
    @POST
    Result<ToolDeveloperInfoVO> getPermissionInfo(
            @Parameter(description = "工具名", required = true)
            @PathParam("toolName")
            String toolName
    );

    @Operation(summary = "同步蓝鲸插件开发者中心的工具成员信息")
    @Path("/syncToolMembers/{toolName}")
    @POST
    Result<Boolean> syncToolMembers(
            @Parameter(description = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @Parameter(description = "工具成员信息", required = true)
            @Valid
            List<ToolMemberInfoVO> toolMemberInfoList
    );
}
