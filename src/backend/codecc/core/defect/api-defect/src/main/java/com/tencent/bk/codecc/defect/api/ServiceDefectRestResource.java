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

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.defect.vo.GrayBuildNumAndTaskVO;
import com.tencent.bk.codecc.defect.vo.GrayDefectStaticVO;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectIdVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectPageVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * 服务间调用的告警管理服务
 *
 * @version V1.0
 * @date 2019/10/20
 */
@Api(tags = {"SERVICE_DEFECT"}, description = "服务间调用的告警管理服务")
@Path("/service/defect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceDefectRestResource {

    @ApiOperation("告警批量处理")
    @Path("/batch/task/{taskId}")
    @POST
    Result<Boolean> batchDefectProcess(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
            long taskId,
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "批量告警处理请求信息", required = true)
            @Valid
            BatchDefectProcessReqVO batchDefectProcessReqVO
    );

    @ApiOperation("按时间获取最后一条告警")
    @Path("/task/{taskId}/tool/{toolName}/lastest")
    @GET
    Result<Long> lastestStatDefect(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
            long taskId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
            String toolName
    );


    @ApiOperation("按任务ID获取最新分析状态")
    @Path("/analyze/status")
    @POST
    Result<Map<Long, String>> getLatestAnalyzeStatus(
            @ApiParam(value = "任务ID集合", required = true) List<Long> taskIds);


    @ApiOperation("通过筛选条件查询告警id")
    @Path("/task/{taskId}/queryDefectId")
    @POST
    Result<List<ToolDefectIdVO>> queryDefectIdByCondition(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
            long taskId,
            @ApiParam(value = "告警查询条件", required = true)
            @Valid
            DefectQueryReqVO reqVO
    );

    @ApiOperation("通过筛选条件聚合工具列表")
    @Path("/task/{taskId}/queryDefectIdPage")
    @POST
    Result<ToolDefectPageVO> queryDefectIdPageByCondition(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
            long taskId,
            @ApiParam(value = "告警查询条件", required = true)
            @Valid
            DefectQueryReqVO reqVO,
            @QueryParam("pageNum")
            int pageNum,
            @QueryParam("pageSize")
            int pageSize

    );

    @ApiOperation("数据是否迁移成功")
    @Path("/commonToLintMigrationSuccessful/{taskId}")
    @GET
    Result<Boolean> commonToLintMigrationSuccessful(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
            long taskId
    );

    @ApiOperation("根据构建号和任务id获取static库中的数据")
    @Path("/getDefectStaticList")
    @POST
    Result<List<GrayDefectStaticVO>> getGaryDefectStaticList(GrayBuildNumAndTaskVO grayBuildNumAndTaskVO);
}
