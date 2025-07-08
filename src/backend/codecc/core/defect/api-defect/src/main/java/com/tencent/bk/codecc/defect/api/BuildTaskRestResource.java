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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Api(tags = {"BUILD_TASK"}, description = "任务信息查询接口")
@Path("/build/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildTaskRestResource {

    @ApiOperation("查询构建快照相关信息")
    @Path("/tasks/{taskId}/buildInfosWithBranches")
    @GET
    Result<List<BuildWithBranchVO>> queryBuildInfosWithBranches(
        @ApiParam(value = "任务ID", required = true)
        @PathParam(value = "taskId")
        Long taskId
    );

    @ApiOperation("查询任务的代码行信息")
    @Path("/tasks/{taskId}/codeLine")
    @GET
    Result<List<TaskCodeLineVO>> getTaskCodeLineInfo(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId
    );
}
