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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.api;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessRspVO;
import com.tencent.bk.codecc.defect.vo.CountDefectFileRequest;
import com.tencent.bk.codecc.defect.vo.FileDefectGatherVO;
import com.tencent.bk.codecc.defect.vo.GetFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.ListToolNameRequest;
import com.tencent.bk.codecc.defect.vo.ListToolNameResponse;
import com.tencent.bk.codecc.defect.vo.PreIgnoreApprovalCheckVO;
import com.tencent.bk.codecc.defect.vo.QueryCheckersAndAuthorsRequest;
import com.tencent.bk.codecc.defect.vo.QueryDefectFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.QueryFileDefectGatherRequest;
import com.tencent.bk.codecc.defect.vo.SingleCommentVO;
import com.tencent.bk.codecc.defect.vo.StatDefectQueryRespVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectRspVO;
import com.tencent.bk.codecc.defect.vo.common.BuildVO;
import com.tencent.bk.codecc.defect.vo.common.BuildWithBranchVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO_Old;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.springframework.data.domain.Sort;

/**
 * 告警查询服务
 */
@Tag(name = "USER_WARN", description = "告警查询服务接口")
@Path("/user/warn")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserDefectRestResource {

    /**
     * 目前在用：CCN、DUPC
     *
     * @param taskId
     * @param toolName
     * @param status
     * @param buildId
     * @return
     */
    @Operation(summary = "初始化告警管理页面的缺陷类型、作者以及树")
    @Path("/checker/authors/toolName/{toolName}")
    @GET
    Result<QueryWarningPageInitRspVO> queryCheckersAndAuthors(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @Parameter(description = "工具名称", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @Parameter(description = "告警状态")
            @QueryParam(value = "status")
            String status,
            @Parameter(description = "构建Id")
            @QueryParam(value = "buildId")
            String buildId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId
    );

    @Operation(summary = "初始化告警管理页面的缺陷类型、作者以及文件树")
    @Path("/checker/authors/list")
    @POST
    Result<QueryWarningPageInitRspVO> queryCheckersAndAuthors(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            QueryCheckersAndAuthorsRequest request
    );

    @Operation(summary = "初始化告警管理页面的缺陷类型、作者以及文件树")
    @Path("/v2/checker/authors/list")
    @POST
    Result<QueryWarningPageInitRspVO> queryCheckersAndAuthorsV2(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            QueryCheckersAndAuthorsRequest request
    );


    @Operation(summary = "查询告警清单")
    @Path("/list")
    @POST
    Result<CommonDefectQueryRspVO> queryDefectList(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            long taskId,
            @Parameter(description = "查询参数详情", required = true)
            @Valid
            DefectQueryReqVO_Old requestVO,
            @Parameter(description = "页数")
            @QueryParam(value = "pageNum")
            int pageNum,
            @Parameter(description = "页面大小")
            @QueryParam(value = "pageSize")
            int pageSize,
            @Parameter(description = "排序字段")
            @QueryParam(value = "sortField")
            String sortField,
            @Parameter(description = "排序方式")
            @QueryParam(value = "sortType")
            Sort.Direction sortType
    );

    @Operation(summary = "查询告警清单(带提单信息)")
    @Path("/issue/list")
    @POST
    Result<CommonDefectQueryRspVO> queryDefectListWithIssue(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "查询参数详情", required = true)
            @Valid
            DefectQueryReqVO defectQueryReqVO,
            @Parameter(description = "页数")
            @QueryParam(value = "pageNum")
            int pageNum,
            @Parameter(description = "页面大小")
            @QueryParam(value = "pageSize")
            int pageSize,
            @Parameter(description = "排序字段")
            @QueryParam(value = "sortField")
            String sortField,
            @Parameter(description = "排序方式")
            @QueryParam(value = "sortType")
            Sort.Direction sortType
    );

    @Operation(summary = "查询告警详情")
    @Path("/detail")
    @POST
    Result<CommonDefectDetailQueryRspVO> queryDefectDetail(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "任务ID", required = false)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @Parameter(description = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @Parameter(description = "查询参数详情", required = true)
            @Valid
            CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO,
            @Parameter(description = "排序字段")
            @QueryParam(value = "sortField")
            String sortField,
            @Parameter(description = "排序方式")
            @QueryParam(value = "sortType")
            Sort.Direction sortType
    );

    @Operation(summary = "查询告警详情(带提单信息)")
    @Path("/issue/detail")
    @POST
    Result<CommonDefectDetailQueryRspVO> queryDefectDetailWithIssue(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "任务ID", required = false)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @Parameter(description = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @Parameter(description = "查询参数详情", required = true)
            @Valid
            CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO,
            @Parameter(description = "排序字段")
            @QueryParam(value = "sortField")
            String sortField,
            @Parameter(description = "排序方式")
            @QueryParam(value = "sortType")
            Sort.Direction sortType
    );

    @Operation(summary = "查询告警详情（不带代码文件片段）")
    @Path("/withoutFileContent/detail")
    @POST
    Result<CommonDefectDetailQueryRspVO> queryDefectDetailWithoutFileContent(
            @Parameter(description = "任务ID", required = false)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @Parameter(description = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @Parameter(description = "查询参数详情", required = true)
            @Valid
            CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO,
            @Parameter(description = "排序字段")
            @QueryParam(value = "sortField")
            String sortField,
            @Parameter(description = "排序方式")
            @QueryParam(value = "sortType")
            Sort.Direction sortType
    );

    @Operation(summary = "查询告警详情（带提单信息, 不带代码文件片段）")
    @Path("/withoutFileContent/issue/detail")
    @POST
    Result<CommonDefectDetailQueryRspVO> queryDefectDetailWithIssueWithoutFileContent(
            @Parameter(description = "任务ID", required = false)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @Parameter(description = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @Parameter(description = "查询参数详情", required = true)
            @Valid
            CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO,
            @Parameter(description = "排序字段")
            @QueryParam(value = "sortField")
            String sortField,
            @Parameter(description = "排序方式")
            @QueryParam(value = "sortType")
            Sort.Direction sortType
    );

    @Operation(summary = "获取告警文件所对应的片段 (极简接口)")
    @Path("/defectFileContentSegment")
    @POST
    Result<CommonDefectDetailQueryRspVO> queryDefectFileContentSegment(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "任务ID", required = false)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @Parameter(description = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @Parameter(description = "获取文件片段相关参数", required = true)
            @Valid
            QueryDefectFileContentSegmentReqVO request);

    @Operation(summary = "获取文件片段")
    @Path("/fileContentSegment")
    @POST
    Result<CommonDefectDetailQueryRspVO> getFileContentSegment(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            long taskId,
            @Parameter(description = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @Parameter(description = "获取文件片段", required = true)
            @Valid
            GetFileContentSegmentReqVO getFileContentSegmentReqVO);

    @Operation(summary = "告警批量处理")
    @Path("/batch")
    @POST
    Result<List<BatchDefectProcessRspVO>> batchDefectProcess(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "批量告警处理请求信息", required = true)
            @Valid
            BatchDefectProcessReqVO batchDefectProcessReqVO
    );

    @Operation(summary = "提前获取忽略审核")
    @Path("/preIgnoreApproval")
    @POST
    Result<PreIgnoreApprovalCheckVO> preCheckIgnoreApproval(
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "批量告警处理请求信息", required = true)
            @Valid
            BatchDefectProcessReqVO batchDefectProcessReqVO
    );

    @Operation(summary = "查询构建列表")
    @Path("/tasks/{taskId}/buildInfos")
    @GET
    Result<List<BuildVO>> queryBuildInfos(
            @Parameter(description = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId
    );

    @Operation(summary = "查询构建快照相关信息")
    @Path("/tasks/{taskId}/buildInfosWithBranches")
    @GET
    Result<List<BuildWithBranchVO>> queryBuildInfosWithBranches(
            @Parameter(description = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId
    );

    @Operation(summary = "运营数据:按条件获取任务告警统计信息")
    @Path("/deptTaskDefect")
    @POST
    Result<DeptTaskDefectRspVO> queryDeptTaskDefect(
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "按组织架构查询任务告警请求", required = true)
            @Valid
            DeptTaskDefectReqVO deptTaskDefectReqVO
    );

    @Operation(summary = "添加代码评论")
    @Path("/codeComment/toolName/{toolName}")
    @POST
    Result<Boolean> addCodeComment(
            @Parameter(description = "告警主键id", required = true)
            @QueryParam(value = "defectId")
            String defectId,
            @Parameter(description = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @Parameter(description = "评论主键id", required = true)
            @QueryParam(value = "commentId")
            String commentId,
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "评论信息", required = true)
            SingleCommentVO singleCommentVO,
            @Parameter(description = "文件名", required = true)
            @QueryParam(value = "fileName")
            String fileName,
            @Parameter(description = "任务名称", required = true)
            @QueryParam(value = "nameCn")
            String nameCn,
            @Parameter(description = "规则", required = true)
            @QueryParam(value = "checker")
            String checker,
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            String taskId
    );

    @Operation(summary = "更新代码评论")
    @Path("/codeComment/commentId/{commentId}/toolName/{toolName}")
    @PUT
    Result<Boolean> updateCodeComment(
            @Parameter(description = "评论主键id", required = true)
            @PathParam(value = "commentId")
            String commentId,
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @Parameter(description = "评论信息", required = true)
            SingleCommentVO singleCommentVO
    );

    @Operation(summary = "删除代码评论")
    @Path("/codeComment/commentId/{commentId}/singleCommentId/{singleCommentId}/toolName/{toolName}")
    @DELETE
    Result<Boolean> deleteCodeComment(
            @Parameter(description = "评论主键id", required = true)
            @PathParam(value = "commentId")
            String commentId,
            @Parameter(description = "单独评论主键id", required = true)
            @PathParam(value = "singleCommentId")
            String singleCommentId,
            @Parameter(description = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "告警实体id")
            @QueryParam(value = "entityId")
            String entityId,
            @Parameter(description = "评论内容")
            @QueryParam(value = "comment")
            String comment
    );

    @Operation(summary = "查询文件告警收敛清单")
    @Path("/gather/taskId/{taskId}/toolName/{toolName}")
    @GET
    Result<FileDefectGatherVO> queryFileDefectGather(
            @Parameter(description = "任务ID", required = true)
            @PathParam(value = "taskId")
            long taskId,
            @Parameter(description = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName
    );

    @Operation(summary = "查询文件告警收敛清单")
    @Path("/queryFileDefectGather")
    @POST
    Result<FileDefectGatherVO> queryFileDefectGather(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            QueryFileDefectGatherRequest request
    );

    @Operation(summary = "查询代码统计清单")
    @Path("/list/toolName/{toolName}/orderBy/{orderBy}")
    @GET
    Result<CommonDefectQueryRspVO> queryCLOCList(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            long taskId,
            @Parameter(description = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @Parameter(description = "数据展示方式", required = true)
            @PathParam(value = "orderBy")
            ComConstants.CLOCOrder orderBy
    );

    @Operation(summary = "告警管理初始化页面")
    @Path("/initpage")
    @POST
    Result<Object> pageInit(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "查询参数详情", required = true)
            @Valid
            DefectQueryReqVO defectQueryReqVO
    );

    @Operation(summary = "查询代码统计清单")
    @Path("/list/tool/stat/toolName/{toolName}")
    @GET
    Result<List<StatDefectQueryRespVO>> queryStatList(
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            long taskId,
            @Parameter(description = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @Parameter(description = "查询范围起始时间", required = false)
            @QueryParam("startTime")
            long startTime,
            @Parameter(description = "查询范围结束时间", required = false)
            @QueryParam(value = "endTime")
            long endTime
    );

    @Operation(summary = "获取当时真实执行过的工具列表(工具可能后期停用了)")
    @Path("/listToolName")
    @POST
    Result<ListToolNameResponse> listToolName(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            ListToolNameRequest request
    );

    @Operation(summary = "数据是否迁移成功")
    @Path("/commonToLintMigrationSuccessful/{taskId}")
    @GET
    Result<Boolean> commonToLintMigrationSuccessful(
            @Parameter(description = "任务ID", required = true)
            @PathParam("taskId")
            long taskId
    );

    @Operation(summary = "统计含告警的文件数")
    @Path("/getNewDefectFileCount")
    @POST
    Result<Long> getNewDefectFileCount(CountDefectFileRequest request);
}