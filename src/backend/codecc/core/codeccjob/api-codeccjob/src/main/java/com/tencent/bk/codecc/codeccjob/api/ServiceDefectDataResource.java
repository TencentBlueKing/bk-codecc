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

package com.tencent.bk.codecc.codeccjob.api;

import com.tencent.devops.common.api.annotation.ServiceInterface;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * 服务间调用的告警管理服务
 *
 * @version V1.0
 * @date 2019/10/20
 */
@Api(tags = {"SERVICE_DATA"}, description = "服务间调用的告警管理服务")
@Path("/service/defect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface(value = "defect")
public interface ServiceDefectDataResource {

    @ApiOperation("迁移数据")
    @Path("/migrate/history")
    @GET
    Result<String> migrateHistoryDefect(
            @ApiParam(value = "任务ID")
            @QueryParam("taskId")
            Long taskId,
            @ApiParam(value = "是否全部")
            @QueryParam("all")
            Boolean all,
            @ApiParam(value = "忽略类型")
            @QueryParam("ignoreType")
            Integer ignoreType,
            @ApiParam(value = "忽略原因")
            @QueryParam("ignoreReason")
            String ignoreReason,
            @ApiParam(value = "用户")
            @QueryParam("user")
            String user
    );

    @ApiOperation("迁移数据")
    @Path("/migrate/history/rollback")
    @GET
    Result<String> rollbackMigrateHistoryDefect(
            @ApiParam(value = "任务ID")
            @QueryParam("taskId")
            Long taskId,
            @ApiParam(value = "是否全部")
            @QueryParam("all")
            Boolean all,
            @ApiParam(value = "忽略类型")
            @QueryParam("ignoreType")
            Integer ignoreType
    );
}
