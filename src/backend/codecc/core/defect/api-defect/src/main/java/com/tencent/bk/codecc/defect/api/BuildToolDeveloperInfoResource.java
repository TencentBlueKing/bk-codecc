package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.ToolMemberInfoVO;
import com.tencent.bk.codecc.defect.vo.developer.ToolDeveloperInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * 工具开发者信息相关的 build 接口
 */
@Api(tags = {"BUILD_TOOL_DEVELOPER"}, description = "工具开发者信息相关的 build 接口")
@Path("/build/tool/developer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildToolDeveloperInfoResource {

    @ApiOperation("获取工具的开发者权限信息")
    @Path("/getPermissionInfo/{toolName}")
    @POST
    Result<ToolDeveloperInfoVO> getPermissionInfo(
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
            String toolName
    );

    @ApiOperation("同步蓝鲸插件开发者中心的工具成员信息")
    @Path("/syncToolMembers/{toolName}")
    @POST
    Result<Boolean> syncToolMembers(
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @ApiParam(value = "工具成员信息", required = true)
            @Valid
            List<ToolMemberInfoVO> toolMemberInfoList
    );
}
