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

package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.GrayToolProjectReqVO;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.bk.codecc.task.vo.TriggerGrayToolVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.bk.codecc.task.vo.GrayToolReportVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;

/**
 * 灰度工具项目调用接口
 *
 * @version V2.0
 * @date 2020/12/29
 */
@Tag(name = "GRAY_TOOL_PROJECT", description = "灰度工具项目调用接口")
@Path("/user/gray/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserGrayToolProjectRestResource {
    @Operation(summary = "灰度项目注册")
    @Path("/projects/register")
    @POST
    Result<Boolean> register(
            @Parameter(description = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId,
            @Parameter(description = "灰度项目数据信息", required = true)
                    GrayToolProjectVO grayToolProjectVO
    );

    @Operation(summary = "灰度项目数据查询列表")
    @Path("/projects/list")
    @POST
    Result<Page<GrayToolProjectVO>> queryGrayToolProjectList(
            @Parameter(description = "按灰度项目查询任务告警请求", required = true) @Valid GrayToolProjectReqVO grayToolProjectReqVO,
            @Parameter(description = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @Parameter(description = "每页多少条") @QueryParam(value = "pageSize") Integer pageSize,
            @Parameter(description = "排序字段") @QueryParam(value = "sortField") String sortField,
            @Parameter(description = "排序类型") @QueryParam(value = "sortType") String sortType
    );

    @Operation(summary = "灰度项目数据更新")
    @Path("/projects/update")
    @POST
    Result<Boolean> updateGrayToolProject(
            @Parameter(description = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId,
            @Parameter(description = "灰度项目数据信息", required = true)
                    GrayToolProjectVO grayToolProjectVO
    );

//    @Operation(summary = "灰度项目数据查询")
//    @Path("/projects/findByProjectId")
//    @GET
//    Result<GrayToolProjectVO> findByProjectId(
//            @Parameter(description = "灰度项目ID", required = true)
//            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
//                    String projectId
//    );

    @Operation(summary = "创建灰度池")
    @Path("/task/pool/toolName/{toolName}/stage/{stage}")
    @POST
    Result<Boolean> createGrayTaskPool(
            @Parameter(description = "工具名", required = true) @PathParam("toolName") String toolName,
            @Parameter(description = "语言", required = false) @QueryParam("langCode") String langCode,
            @Parameter(description = "阶段", required = true) @PathParam("stage") String stage,
            @Parameter(description = "当前用户", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String user);

    @Operation(summary = "触发灰度池项目")
    @Path("/task/pool/trigger/toolName/{toolName}/taskNum")
    @GET
    Result<TriggerGrayToolVO> triggerGrayTaskPool(
            @Parameter(description = "工具名", required = true) @PathParam("toolName") String toolName,
            @Parameter(description = "执行任务数", required = false) @QueryParam("taskNum") String taskNum,
            @Parameter(description = "语言", required = false) @QueryParam("langCode") String langCode
    );

//    @Operation(summary = "根据工具名查询灰度项目信息")
//    @Path("/project/toolName/{toolName}")
//    @GET
//    Result<GrayToolProjectVO> findGrayToolProjInfoByToolName(
//            @Parameter(description = "工具名", required = true)
//            @PathParam("toolName")
//                    String toolName);
}
