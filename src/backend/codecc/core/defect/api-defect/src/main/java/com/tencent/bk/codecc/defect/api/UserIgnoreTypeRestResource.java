package com.tencent.bk.codecc.defect.api;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeDefectStatResponse;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeProjectConfigVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeSysVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * 忽略类型-用户接口
 */
@Api(tags = {"USER_IGNORE"}, description = "忽略类型")
@Path("/user/ignoreType")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserIgnoreTypeRestResource {

    @ApiOperation("创建")
    @Path("/project/save")
    @POST
    Result<Boolean> save(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "项目忽略配置", required = true)
            IgnoreTypeProjectConfigVO projectConfig
    );

    @ApiOperation("更新状态")
    @Path("/project/updateStatus")
    @POST
    Result<Boolean> updateStatus(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "项目忽略配置", required = true)
            IgnoreTypeProjectConfigVO projectConfig
    );

    @ApiOperation("默认忽略类型配置列表")
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
    @ApiOperation("获取项目忽略类型列表")
    @Path("/project/list")
    @GET
    Result<List<IgnoreTypeProjectConfigVO>> list(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );

    @ApiOperation("获取详情")
    @Path("/project/detail")
    @GET
    Result<IgnoreTypeProjectConfigVO> detail(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @QueryParam("ignoreTypeId")
            @ApiParam(value = "忽略类型ID", required = true)
            Integer ignoreTypeId
    );

    @ApiOperation("获取忽略类型告警告警统计")
    @Path("/project/defect/stat")
    @GET
    Result<List<IgnoreTypeDefectStatResponse>> defectStat(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );


    @ApiOperation("")
    @Path("/project/hasAddPermissions")
    @GET
    Result<Boolean> hasAddPermissions(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "用户名称", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );
}
