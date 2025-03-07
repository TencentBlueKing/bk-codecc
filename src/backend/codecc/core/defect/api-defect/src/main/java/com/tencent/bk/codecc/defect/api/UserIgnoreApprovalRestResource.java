package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.ignore.IgnoreApprovalConfigVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 忽略类型-用户接口
 */
@Api(tags = {"USER_IGNORE_APPROVAL"}, description = "忽略审核")
@Path("/user/ignoreApproval")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserIgnoreApprovalRestResource {

    /**
     * 创建单项目的忽略审批
     *
     * @param projectId
     * @param userName
     * @param ignoreApprovalConfigVO
     * @return
     */
    @ApiOperation("创建")
    @Path("/project/saveConfig")
    @POST
    Result<Boolean> saveConfig(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "项目忽略配置", required = true)
            IgnoreApprovalConfigVO ignoreApprovalConfigVO
    );

    /**
     * 获取项目符合的审批配置
     * 单项目 OR 多项目
     *
     * @param projectId
     * @param userName
     * @return
     */
    @ApiOperation("获取项目忽略类型列表")
    @Path("/project/config/list")
    @GET
    Result<Page<IgnoreApprovalConfigVO>> configList(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "页数")
            @QueryParam(value = "pageNum")
            Integer pageNum,
            @ApiParam(value = "每页多少条")
            @QueryParam(value = "pageSize")
            Integer pageSize
    );

    /**
     * 仅可以获取单项目的
     *
     * @param projectId
     * @param userName
     * @param ignoreApprovalId 忽略审批配置ID
     * @return
     */
    @ApiOperation("获取详情")
    @Path("/project/config/detail")
    @GET
    Result<IgnoreApprovalConfigVO> configDetail(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @QueryParam("ignoreApprovalId")
            @ApiParam(value = "忽略审批配置ID", required = true)
            String ignoreApprovalId
    );

    /**
     * 删除项目配置
     *
     * @param projectId
     * @param userName
     * @param ignoreApprovalId 忽略审批配置ID
     * @return
     */
    @ApiOperation("删除项目配置")
    @Path("/project/config/delete")
    @PUT
    Result<Boolean> configDelete(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @QueryParam("ignoreApprovalId")
            @ApiParam(value = "忽略审批配置ID", required = true)
            String ignoreApprovalId
    );

}
