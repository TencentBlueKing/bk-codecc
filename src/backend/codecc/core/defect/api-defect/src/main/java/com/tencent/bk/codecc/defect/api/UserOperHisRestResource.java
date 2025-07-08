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

import com.tencent.bk.codecc.defect.vo.OperationHistoryVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 操作记录接口
 *
 * @version V1.0
 * @date 2019/6/18
 */
@Api(tags = {"USER_OPERATION_HISTORY"}, description = "操作记录接口")
@Path("/user/operation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserOperHisRestResource
{
    @ApiOperation("获取操作记录清单")
    @Path("/taskId/{taskId}")
    @POST
    Result<List<OperationHistoryVO>> getOperHisRecord(
            @ApiParam(value = "任务id", required = true)
            @PathParam("taskId")
                    long taskId,
            @ApiParam(value = "工具名")
            @QueryParam("toolName")
                    String toolName,
            @ApiParam("功能id清单")
                    List<String> funcId
    );

    @ApiOperation("设置codeCC任务用户权限操作记录")
    @Path("/settings/authority")
    @POST
    Result<Boolean> settingsAuthorityOperationHistory(
            @ApiParam("触发参数") List<String> reqVOList,
            @ApiParam(value = "用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userId,
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID) long taskId
    );
}
