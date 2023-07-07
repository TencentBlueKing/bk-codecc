/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * bkcheck工具侧获取任务分析记录接口
 * @author jamikxu
 * @version V1.0
 * @date 2023/2/20
 */
@Api(tags = {"BKCHECK_TASKLOG"}, description = "bkcheck工具侧获取任务分析记录接口")
@Path("/build/bkcheck/tasklog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildBkcheckTaskLogRestResource {

    @ApiOperation("获取最近一次工具构建成功的分析记录")
    @Path("/taskId/{taskId}/toolName/{toolName}")
    @GET
    Result<TaskLogVO> getBuildTaskLog(
            @ApiParam(value = "任务id", required = true)
            @PathParam("taskId")
            long taskId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
            String toolName
    );
}
