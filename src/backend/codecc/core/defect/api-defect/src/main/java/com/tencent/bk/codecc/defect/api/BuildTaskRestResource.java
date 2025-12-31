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

import com.tencent.bk.codecc.defect.vo.common.BuildWithBranchVO;
import com.tencent.bk.codecc.defect.vo.common.TaskCodeLineVO;
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
import java.util.List;

@Tag(name = "BUILD_TASK", description = "任务信息查询接口")
@Path("/build/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildTaskRestResource {

    @Operation(summary = "查询构建快照相关信息")
    @Path("/tasks/{taskId}/buildInfosWithBranches")
    @GET
    Result<List<BuildWithBranchVO>> queryBuildInfosWithBranches(
        @Parameter(description = "任务ID", required = true)
        @PathParam(value = "taskId")
        Long taskId
    );

    @Operation(summary = "查询任务的代码行信息")
    @Path("/tasks/{taskId}/codeLine")
    @GET
    Result<List<TaskCodeLineVO>> getTaskCodeLineInfo(
            @Parameter(description = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId
    );
}
