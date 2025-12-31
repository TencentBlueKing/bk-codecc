package com.tencent.bk.codecc.defect.api;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeDefectStatResponse;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeProjectConfigVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeSysVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * 忽略类型-用户接口
 */
@Tag(name = "USER_IGNORE", description = "忽略类型")
@Path("/user/ignoreType")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserIgnoreTypeRestResource {

    @Operation(summary = "创建")
    @Path("/project/save")
    @POST
    Result<Boolean> save(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "项目忽略配置", required = true)
            IgnoreTypeProjectConfigVO projectConfig
    );

    @Operation(summary = "更新状态")
    @Path("/project/updateStatus")
    @POST
    Result<Boolean> updateStatus(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "项目忽略配置", required = true)
            IgnoreTypeProjectConfigVO projectConfig
    );

    @Operation(summary = "默认忽略类型配置列表")
    @Path("/sys/list")
    @GET
    Result<List<IgnoreTypeSysVO>> queryIgnoreTypeSysList();


    /**
     * 获取项目配置的忽略列表
     * 系统配置 + 用户配置
     *
     * @param projectId
     * @param userName
     * @return
     */
    @Operation(summary = "获取项目忽略类型列表")
    @Path("/project/list")
    @GET
    Result<List<IgnoreTypeProjectConfigVO>> list(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );

    @Operation(summary = "获取详情")
    @Path("/project/detail")
    @GET
    Result<IgnoreTypeProjectConfigVO> detail(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @QueryParam("ignoreTypeId")
            @Parameter(description = "忽略类型ID", required = true)
            Integer ignoreTypeId
    );

    @Operation(summary = "获取忽略类型告警告警统计")
    @Path("/project/defect/stat")
    @GET
    Result<List<IgnoreTypeDefectStatResponse>> defectStat(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );


        @Path("/project/hasAddPermissions")
    @GET
    Result<Boolean> hasAddPermissions(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );
}
