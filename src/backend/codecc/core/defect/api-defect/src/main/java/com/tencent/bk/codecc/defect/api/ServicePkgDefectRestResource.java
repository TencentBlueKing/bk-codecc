/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.CLOCDefectQueryRspInfoVO;
import com.tencent.bk.codecc.defect.vo.ToolClocRspVO;
import com.tencent.bk.codecc.defect.vo.openapi.TaskOverviewDetailRspVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.domain.Sort;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * 告警相关接口
 * 
 * @date 2019/11/15
 * @version V1.0
 */
@Tag(name = "SERVICE_PKGDEFECT", description = "告警相关接口")
@Path("/service/pkgDefect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServicePkgDefectRestResource {

    @Operation(summary = "查询代码行数信息")
    @Path("/codeLine/taskId/{taskId}")
    @POST
    Result<ToolClocRspVO> queryCodeLine(
            @Parameter(description = "任务id", required = true)
            @PathParam(value = "taskId")
            Long taskId,
            @Parameter(description = "工具名称", required = true)
            @QueryParam(value = "toolName")
            @DefaultValue("SCC")
            String toolName);


    @Operation(summary = "通过task_id和语言查询代码行信息")
    @Path("/codeLine/taskId/{taskId}/toolName/{toolName}/language/{language}")
    @GET
    Result<CLOCDefectQueryRspInfoVO> queryCodeLineByTaskIdAndLanguge(
            @Parameter(description = "任务id", required = true)
            @PathParam(value = "taskId")
                    Long taskId,
            @Parameter(description = "工具名称", required = true)
            @PathParam(value = "toolName")
                    String toolName,
            @Parameter(description = "语言", required = true)
            @PathParam(value = "language")
                    String language);
}
