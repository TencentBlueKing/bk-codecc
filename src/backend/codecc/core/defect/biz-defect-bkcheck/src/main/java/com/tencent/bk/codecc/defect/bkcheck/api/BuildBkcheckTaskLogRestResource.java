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

package com.tencent.bk.codecc.defect.bkcheck.api;

import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * bkcheck工具侧获取任务分析记录接口
 * @author jamikxu
 * @version V1.0
 * @date 2023/2/20
 */
@Tag(name = "BKCHECK_TASKLOG", description = "bkcheck工具侧获取任务分析记录接口")
@Path("/build/bkcheck/tasklog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildBkcheckTaskLogRestResource {

    @Operation(summary = "获取最近一次工具构建成功的分析记录")
    @Path("/taskId/{taskId}/toolName/{toolName}")
    @GET
    Result<TaskLogVO> getBuildTaskLog(
            @Parameter(description = "任务id", required = true)
            @PathParam("taskId")
            long taskId,
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
            String toolName
    );
}
