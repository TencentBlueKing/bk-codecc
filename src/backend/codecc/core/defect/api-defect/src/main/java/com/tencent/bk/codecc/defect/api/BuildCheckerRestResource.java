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

import com.tencent.bk.codecc.defect.vo.CheckerImportVO;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_BUILD_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 规则导入接口
 *
 * @version V2.0
 * @date 2020/4/8
 */
@Tag(name = "BUILD_CHECKER", description = "规则导入接口")
@Path("/build/checker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildCheckerRestResource {

    @Operation(summary = "导入规则")
    @Path("/")
    @POST
    Result<Map<String, List<CheckerPropVO>>> checkerImport(
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "项目名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "规则导入请求对象", required = true)
            CheckerImportVO checkerImportVO
    );

    @Operation(summary = "更新规则集状态元数据")
    @Path("/tools/{toolName}/integratedStatus/update")
    @PUT
    Result<List<String>> updateToolCheckersToStatus(
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "buildId", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
            String buildId,
            @Parameter(description = "工具名称")
            @PathParam("toolName")
            String toolName,
            @Parameter(description = "源状态")
            @QueryParam("fromStatus")
            ToolIntegratedStatus fromStatus,
            @Parameter(description = "目前状态")
            @QueryParam("toStatus")
            ToolIntegratedStatus toStatus
    );

    @Operation(summary = "回滚规则集状态元数据")
    @Path("/tools/{toolName}/integratedStatus/revert")
    @PUT
    Result<String> revertToolCheckerSetStatus(
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "工具名称")
            @PathParam("toolName")
            String toolName,
            @Parameter(description = "状态")
            @QueryParam("status")
            ToolIntegratedStatus status
    );

    @Operation(summary = "获取工具的规则数量")
    @Path("/get/checkerNum/{toolName}")
    @GET
    Result<Long> getCheckerNumByToolName(
            @Parameter(description = "工具名称")
            @PathParam("toolName")
            String toolName
    );
}
