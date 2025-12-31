/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.defect.vo.checkerset.AddCheckerSet2TaskReqVO;
import com.tencent.bk.codecc.defect.vo.checkerset.CheckerSetDifferenceVO;
import com.tencent.bk.codecc.defect.vo.checkerset.UpdateCheckerSetReqVO;
import com.tencent.bk.codecc.defect.vo.checkerset.UserCreatedCheckerSetsVO;
import com.tencent.bk.codecc.defect.vo.enums.CheckerListSortType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerPermissionType;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.domain.Sort;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 配置规则包服务
 *
 * @version V1.0
 * @date 2019/5/29
 */
@Tag(name = "USER_CHECKER", description = " 配置规则包接口")
@Path("/user/checker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserCheckerRestResource {

    @Operation(summary = "获取配置规则包")
    @Path("/tasks/{taskId}/toolName/{toolName}/checkers")
    @GET
    Result<GetCheckerListRspVO> checkerPkg(
            @Parameter(description = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName
    );


    @Operation(summary = "打开或者关闭配置规则包")
    @Path("/tasks/{taskId}/toolName/{toolName}/checkers/configuration")
    @POST
    Result<Boolean> configCheckerPkg(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @Parameter(description = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName,
            @Parameter(description = "配置规则包参数", required = true)
                    ConfigCheckersPkgReqVO packageVo
    );

    @Operation(summary = "修改规则集")
    @Path("/tasks/{taskId}/tools/{toolName}/checkerSets/{checkerSetId}")
    @PUT
    Result<Boolean> updateCheckerSet(
            @Parameter(description = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName,
            @Parameter(description = "规则集ID", required = true)
            @PathParam("checkerSetId")
                    String checkerSetId,
            @Parameter(description = "修改规则集请求参数", required = true)
                    UpdateCheckerSetReqVO updateCheckerSetReqVO,
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId
    );


    @Operation(summary = "任务关联规则集")
    @Path("/tasks/{taskId}/checkerSets/relationship")
    @POST
    Result<Boolean> addCheckerSet2Task(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @Parameter(description = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "任务关联规则集请求参数", required = true)
                    AddCheckerSet2TaskReqVO addCheckerSet2TaskReqVO
    );


    @Operation(summary = "查询用户创建的规则集列表")
    @Path("/tools/{toolName}/userCreatedCheckerSets")
    @GET
    Result<UserCreatedCheckerSetsVO> getUserCreatedCheckerSet(
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName,
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId
    );


    @Operation(summary = "查询规则集指定版本的差异")
    @Path("/tools/{toolName}/checkerSets/{checkerSetId}/versions/difference")
    @POST
    Result<CheckerSetDifferenceVO> getCheckerSetVersionDifference(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName,
            @Parameter(description = "规则集ID", required = true)
            @PathParam("checkerSetId")
                    String checkerSetId,
            @Parameter(description = "规则集指定版本差异请求体", required = true)
                    CheckerSetDifferenceVO checkerSetDifferenceVO
    );

    @Operation(summary = "更新规则参数配置")
    @Path("/taskId/{taskId}/tools/{toolName}/param/{paramValue}")
    @PUT
    Result<Boolean> updateCheckerConfigParam(
            @Parameter(description = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName,
            @Parameter(description = "规则键", required = true)
            @QueryParam("checkerKey")
                    String checkerName,
            @Parameter(description = "参数值", required = true)
            @PathParam("paramValue")
                    String paramValue,
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user
    );


    @Operation(summary = "获取规则详情")
    @Path("/detail/toolName/{toolName}")
    @GET
    Result<CheckerDetailVO> queryCheckerDetail(
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName,
            @Parameter(description = "规则键", required = true)
            @QueryParam("checkerKey")
                    String checkerKey
    );

    @Operation(summary = "获取规则详情")
    @Path("/list")
    @POST
    Result<List<CheckerDetailVO>> queryCheckerDetailList(
            @Parameter(description = "规则清单查询条件", required = true)
                    CheckerListQueryReq checkerListQueryReq,
            @Parameter(description = "项目id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @Parameter(description = "页数")
            @QueryParam("pageNum")
                    Integer pageNum,
            @Parameter(description = "页数")
            @QueryParam("pageSize")
                    Integer pageSize,
            @Parameter(description = "升序或降序")
            @QueryParam("sortType")
                    Sort.Direction sortType,
            @Parameter(description = "排序字段")
            @QueryParam("sortField")
                    CheckerListSortType sortField);

    @Operation(summary = "获取规则数量")
    @Path("/count")
    @POST
    Result<List<CheckerCommonCountVO>> queryCheckerCountList(
            @Parameter(description = "规则数量查询条件", required = true)
                    CheckerListQueryReq checkerListQueryReq,
            @Parameter(description = "项目id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId);

    @Operation(summary = "获取规则详情")
    @Path("/toolName/{toolName}/queryChecker")
    @GET
    Result<List<CheckerDetailVO>> queryCheckerByTool(
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName
    );

    /**
     * 根据checkerKey和ToolName更新规则详情
     *
     * @param checkerDetailVO
     * @return
     */
    @Operation(summary = "编辑规则详情")
    @Path("/update")
    @POST
    Result<Boolean> updateCheckerByCheckerKey(
            @Parameter(description = "规则详情请求体", required = true)
                    CheckerDetailVO checkerDetailVO,
            @Parameter(description = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId
            );

    @Operation(summary = "导入用户自定义规则")
    @Path("/custom")
    @POST
    Result<Boolean> customCheckerImport(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        String userName,
        @Parameter(description = "项目名", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        String projectId,
        @Parameter(description = "规则导入请求对象", required = true)
        CheckerImportVO checkerImportVO
    );

    @Operation(summary = "编辑用户自定义规则详情")
    @Path("/updateCustomChecker")
    @POST
    Result<Boolean> updateCustomCheckerByCheckerKey(
        @Parameter(description = "规则详情请求体", required = true)
        CheckerDetailVO checkerDetailVO,
        @Parameter(description = "项目id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        String projectId,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        String userId
    );

    /**
     * 根据checkerKey和ToolName删除用户自定义规则
     *
     * @param checkerDetailVO
     * @deprecated 由于规则集的更新采用累加版本方式，删除规则对规则集的处理逻辑将导致原有设计遭到破坏，目前暂时停用删除功能
     * @return
     */
    @Operation(summary = "删除用户自定义规则")
    @Path("/deleteCustomChecker")
    @POST
    @Deprecated
    Result<Boolean> deleteCustomCheckerByCheckerKey(
        @Parameter(description = "规则详情请求体", required = true)
        CheckerDetailVO checkerDetailVO,
        @Parameter(description = "项目id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        String projectId,
        @Parameter(description = "用户id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        String userId
    );

    @Operation(summary = "获取规则详情列表")
    @Path("/list/preci")
    @POST
    Result<List<CheckerDetailVO>> queryCheckerDetailListForPreCI(
            @Parameter(description = "规则清单查询条件", required = true)
                    CheckerDetailListQueryReqVO checkerListQueryReq
    );

    @Operation(summary = "判断用户是否具有管理项目下的规则权限")
    @Path("/userManagementPermission")
    @POST
    Result<List<CheckerPermissionType>> getCheckerManagementPermission(
        @Parameter(description = "规则权限校验条件", required = true)
        CheckerManagementPermissionReqVO authManagementPermissionReqVO
    );

}
