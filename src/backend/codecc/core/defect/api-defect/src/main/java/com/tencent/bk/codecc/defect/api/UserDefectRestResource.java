/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.springframework.data.domain.Sort;

/**
 * 告警查询服务
 */
@Api(tags = {"USER_WARN"}, description = "告警查询服务接口")
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
    @ApiOperation("初始化告警管理页面的缺陷类型、作者以及树")
    @Path("/checker/authors/toolName/{toolName}")
    @GET
    Result<QueryWarningPageInitRspVO> queryCheckersAndAuthors(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @ApiParam(value = "告警状态")
            @QueryParam(value = "status")
            String status,
            @ApiParam(value = "构建Id")
            @QueryParam(value = "buildId")
            String buildId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId
    );

    @ApiOperation("初始化告警管理页面的缺陷类型、作者以及树")
    @Path("/checker/authors/list")
    @GET
    Result<QueryWarningPageInitRspVO> queryCheckersAndAuthors(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @ApiParam(value = "工具名称，数据迁移后支持多选，逗号分割多个", required = false)
            @QueryParam(value = "toolName")
            String toolName,
            @ApiParam(value = "维度，数据迁移后支持多选，逗号分割多个", required = false)
            @QueryParam(value = "dimension")
            String dimension,
            @ApiParam(value = "告警状态")
            @QueryParam(value = "status")
            String status,
            @ApiParam(value = "规则及名称", required = false)
            @QueryParam(value = "checkerSet")
            String checkerSet,
            @ApiParam(value = "构建Id", required = false)
            @QueryParam(value = "buildId")
            String buildId
    );

    @ApiOperation("初始化告警管理页面的缺陷类型、作者以及文件树")
    @Path("/checker/authors/list")
    @POST
    Result<QueryWarningPageInitRspVO> queryCheckersAndAuthors(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            QueryCheckersAndAuthorsRequest request
    );


    @ApiOperation("查询告警清单")
    @Path("/list")
    @POST
    Result<CommonDefectQueryRspVO> queryDefectList(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            long taskId,
            @ApiParam(value = "查询参数详情", required = true)
            @Valid
            DefectQueryReqVO_Old requestVO,
            @ApiParam(value = "页数")
            @QueryParam(value = "pageNum")
            int pageNum,
            @ApiParam(value = "页面大小")
            @QueryParam(value = "pageSize")
            int pageSize,
            @ApiParam(value = "排序字段")
            @QueryParam(value = "sortField")
            String sortField,
            @ApiParam(value = "排序方式")
            @QueryParam(value = "sortType")
            Sort.Direction sortType
    );

    @ApiOperation("查询告警清单(带提单信息)")
    @Path("/issue/list")
    @POST
    Result<CommonDefectQueryRspVO> queryDefectListWithIssue(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "查询参数详情", required = true)
            @Valid
            DefectQueryReqVO defectQueryReqVO,
            @ApiParam(value = "页数")
            @QueryParam(value = "pageNum")
            int pageNum,
            @ApiParam(value = "页面大小")
            @QueryParam(value = "pageSize")
            int pageSize,
            @ApiParam(value = "排序字段")
            @QueryParam(value = "sortField")
            String sortField,
            @ApiParam(value = "排序方式")
            @QueryParam(value = "sortType")
            Sort.Direction sortType
    );

    @ApiOperation("查询告警详情")
    @Path("/detail")
    @POST
    Result<CommonDefectDetailQueryRspVO> queryDefectDetail(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "任务ID", required = false)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @ApiParam(value = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "查询参数详情", required = true)
            @Valid
            CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO,
            @ApiParam(value = "排序字段")
            @QueryParam(value = "sortField")
            String sortField,
            @ApiParam(value = "排序方式")
            @QueryParam(value = "sortType")
            Sort.Direction sortType
    );

    @ApiOperation("查询告警详情(带提单信息)")
    @Path("/issue/detail")
    @POST
    Result<CommonDefectDetailQueryRspVO> queryDefectDetailWithIssue(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "任务ID", required = false)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @ApiParam(value = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "查询参数详情", required = true)
            @Valid
            CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO,
            @ApiParam(value = "排序字段")
            @QueryParam(value = "sortField")
            String sortField,
            @ApiParam(value = "排序方式")
            @QueryParam(value = "sortType")
            Sort.Direction sortType
    );

    @ApiOperation("查询告警详情（不带代码文件片段）")
    @Path("/withoutFileContent/detail")
    @POST
    Result<CommonDefectDetailQueryRspVO> queryDefectDetailWithoutFileContent(
            @ApiParam(value = "任务ID", required = false)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @ApiParam(value = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "查询参数详情", required = true)
            @Valid
            CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO,
            @ApiParam(value = "排序字段")
            @QueryParam(value = "sortField")
            String sortField,
            @ApiParam(value = "排序方式")
            @QueryParam(value = "sortType")
            Sort.Direction sortType
    );

    @ApiOperation("查询告警详情（带提单信息, 不带代码文件片段）")
    @Path("/withoutFileContent/issue/detail")
    @POST
    Result<CommonDefectDetailQueryRspVO> queryDefectDetailWithIssueWithoutFileContent(
            @ApiParam(value = "任务ID", required = false)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @ApiParam(value = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "查询参数详情", required = true)
            @Valid
            CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO,
            @ApiParam(value = "排序字段")
            @QueryParam(value = "sortField")
            String sortField,
            @ApiParam(value = "排序方式")
            @QueryParam(value = "sortType")
            Sort.Direction sortType
    );

    @ApiOperation("获取告警文件所对应的片段 (极简接口)")
    @Path("/defectFileContentSegment")
    @POST
    Result<CommonDefectDetailQueryRspVO> queryDefectFileContentSegment(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "任务ID", required = false)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @ApiParam(value = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "获取文件片段相关参数", required = true)
            @Valid
            QueryDefectFileContentSegmentReqVO request);

    @ApiOperation("获取文件片段")
    @Path("/fileContentSegment")
    @POST
    Result<CommonDefectDetailQueryRspVO> getFileContentSegment(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            long taskId,
            @ApiParam(value = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "获取文件片段", required = true)
            @Valid
            GetFileContentSegmentReqVO getFileContentSegmentReqVO);

    @ApiOperation("告警批量处理")
    @Path("/batch")
    @POST
    Result<List<BatchDefectProcessRspVO>> batchDefectProcess(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "批量告警处理请求信息", required = true)
            @Valid
            BatchDefectProcessReqVO batchDefectProcessReqVO
    );

    @ApiOperation("查询构建列表")
    @Path("/tasks/{taskId}/buildInfos")
    @GET
    Result<List<BuildVO>> queryBuildInfos(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId
    );

    @ApiOperation("查询构建快照相关信息")
    @Path("/tasks/{taskId}/buildInfosWithBranches")
    @GET
    Result<List<BuildWithBranchVO>> queryBuildInfosWithBranches(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId
    );

    @ApiOperation("运营数据:按条件获取任务告警统计信息")
    @Path("/deptTaskDefect")
    @POST
    Result<DeptTaskDefectRspVO> queryDeptTaskDefect(
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "按组织架构查询任务告警请求", required = true)
            @Valid
            DeptTaskDefectReqVO deptTaskDefectReqVO
    );

    @ApiOperation("添加代码评论")
    @Path("/codeComment/toolName/{toolName}")
    @POST
    Result<Boolean> addCodeComment(
            @ApiParam(value = "告警主键id", required = true)
            @QueryParam(value = "defectId")
            String defectId,
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @ApiParam(value = "评论主键id", required = true)
            @QueryParam(value = "commentId")
            String commentId,
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "评论信息", required = true)
            SingleCommentVO singleCommentVO,
            @ApiParam(value = "文件名", required = true)
            @QueryParam(value = "fileName")
            String fileName,
            @ApiParam(value = "任务名称", required = true)
            @QueryParam(value = "nameCn")
            String nameCn,
            @ApiParam(value = "规则", required = true)
            @QueryParam(value = "checker")
            String checker,
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            String taskId
    );

    @ApiOperation("更新代码评论")
    @Path("/codeComment/commentId/{commentId}/toolName/{toolName}")
    @PUT
    Result<Boolean> updateCodeComment(
            @ApiParam(value = "评论主键id", required = true)
            @PathParam(value = "commentId")
            String commentId,
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @ApiParam(value = "评论信息", required = true)
            SingleCommentVO singleCommentVO
    );

    @ApiOperation("删除代码评论")
    @Path("/codeComment/commentId/{commentId}/singleCommentId/{singleCommentId}/toolName/{toolName}")
    @DELETE
    Result<Boolean> deleteCodeComment(
            @ApiParam(value = "评论主键id", required = true)
            @PathParam(value = "commentId")
            String commentId,
            @ApiParam(value = "单独评论主键id", required = true)
            @PathParam(value = "singleCommentId")
            String singleCommentId,
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "告警实体id")
            @QueryParam(value = "entityId")
            String entityId,
            @ApiParam(value = "评论内容")
            @QueryParam(value = "comment")
            String comment
    );

    @ApiOperation("查询文件告警收敛清单")
    @Path("/gather/taskId/{taskId}/toolName/{toolName}")
    @GET
    Result<FileDefectGatherVO> queryFileDefectGather(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
            long taskId,
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName
    );

    @ApiOperation("查询文件告警收敛清单")
    @Path("/queryFileDefectGather")
    @POST
    Result<FileDefectGatherVO> queryFileDefectGather(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            QueryFileDefectGatherRequest request
    );

    @ApiOperation("查询代码统计清单")
    @Path("/list/toolName/{toolName}/orderBy/{orderBy}")
    @GET
    Result<CommonDefectQueryRspVO> queryCLOCList(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            long taskId,
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @ApiParam(value = "数据展示方式", required = true)
            @PathParam(value = "orderBy")
            ComConstants.CLOCOrder orderBy
    );

    @ApiOperation("告警管理初始化页面")
    @Path("/initpage")
    @POST
    Result<Object> pageInit(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "查询参数详情", required = true)
            @Valid
            DefectQueryReqVO defectQueryReqVO
    );

    @ApiOperation("查询代码统计清单")
    @Path("/list/tool/stat/toolName/{toolName}")
    @GET
    Result<List<StatDefectQueryRespVO>> queryStatList(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            long taskId,
            @ApiParam(value = "工具名", required = true)
            @PathParam(value = "toolName")
            String toolName,
            @ApiParam(value = "查询范围起始时间", required = false)
            @QueryParam("startTime")
            long startTime,
            @ApiParam(value = "查询范围结束时间", required = false)
            @QueryParam(value = "endTime")
            long endTime
    );

    @ApiOperation("获取当时真实执行过的工具列表(工具可能后期停用了)")
    @Path("/listToolName")
    @POST
    Result<ListToolNameResponse> listToolName(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            ListToolNameRequest request
    );

    @ApiOperation("数据是否迁移成功")
    @Path("/commonToLintMigrationSuccessful/{taskId}")
    @GET
    Result<Boolean> commonToLintMigrationSuccessful(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
            long taskId
    );

    @ApiOperation("统计含告警的文件数")
    @Path("/getNewDefectFileCount")
    @POST
    Result<Long> getNewDefectFileCount(CountDefectFileRequest request);
}