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

import com.tencent.bk.codecc.defect.vo.UpdateDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.devops.common.api.annotation.ServiceInterface;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.Set;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * 后台微服务（如coverity）告警上报服务接口
 *
 * @version V1.0
 * @date 2019/11/2
 */
@Tag(name = "SERVICE_DEFECT", description = "后台微服务（如coverity）告警上报服务接口")
@Path("/service/defects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface(value = "report")
public interface ServiceReportDefectRestResource {

    @Operation(summary = "查询所有的告警ID")
    @Path("/ids/taskId/{taskId}/toolName/{toolName}")
    @GET
    Result<Set<Long>> queryIds(
            @Parameter(description = "任务ID", required = true)
            @PathParam("taskId")
            long taskId,
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
            String toolName,
            @Parameter(description = "数据是否迁移成功", required = false)
            @QueryParam("migrationSuccessful")
            Boolean migrationSuccessful
    );

    @Operation(summary = "批量更新告警状态")
    @Path("/status")
    @PUT
    Result updateDefectStatus(
            @Parameter(description = "告警状态映射表", required = true)
            UpdateDefectVO updateDefectVO);

    @Operation(summary = "上报告警")
    @Path("/")
    @POST
    Result reportDefects(
            @Parameter(description = "告警详细信息", required = true)
            UploadDefectVO uploadDefectVO);

    @Operation(summary = "更新告警详情")
    @Path("/update/detail")
    @POST
    Result updateDefects(
            @Parameter(description = "告警详细信息", required = true)
            UpdateDefectVO updateDefectVO);
}
