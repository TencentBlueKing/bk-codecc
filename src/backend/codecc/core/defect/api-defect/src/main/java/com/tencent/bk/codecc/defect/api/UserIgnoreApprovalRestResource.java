package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.ignore.IgnoreApprovalConfigVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 忽略类型-用户接口
 */
@Tag(name = "USER_IGNORE_APPROVAL", description = "忽略审核")
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
    @Operation(summary = "创建")
    @Path("/project/saveConfig")
    @POST
    Result<Boolean> saveConfig(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "项目忽略配置", required = true)
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
    @Operation(summary = "获取项目忽略类型列表")
    @Path("/project/config/list")
    @GET
    Result<Page<IgnoreApprovalConfigVO>> configList(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "页数")
            @QueryParam(value = "pageNum")
            Integer pageNum,
            @Parameter(description = "每页多少条")
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
    @Operation(summary = "获取详情")
    @Path("/project/config/detail")
    @GET
    Result<IgnoreApprovalConfigVO> configDetail(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @QueryParam("ignoreApprovalId")
            @Parameter(description = "忽略审批配置ID", required = true)
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
    @Operation(summary = "删除项目配置")
    @Path("/project/config/delete")
    @PUT
    Result<Boolean> configDelete(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @QueryParam("ignoreApprovalId")
            @Parameter(description = "忽略审批配置ID", required = true)
            String ignoreApprovalId
    );

}
