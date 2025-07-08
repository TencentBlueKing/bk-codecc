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

import com.tencent.bk.codecc.defect.vo.coderepository.UploadRepositoriesVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.data.domain.Sort;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 工具侧告警上报服务
 *
 * @version V1.0
 * @date 2019/5/2
 */
@Api(tags = {"SERVICE_DEFECT"}, description = "工具侧告警上报服务接口")
@Path("/build/defects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildDefectRestResource
{
    @ApiOperation("工具侧上报代码仓库信息")
    @Path("/repositories")
    @POST
    Result uploadRepositories(
            @ApiParam(value = "工具侧上报代码仓库信息", required = true)
                    UploadRepositoriesVO uploadRepositoriesVO);


    @ApiOperation("查询告警清单(带提单信息)")
    @Path("/issue/list")
    @POST
    Result<CommonDefectQueryRspVO> queryDefectListWithIssue(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        String userId,
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        String projectId,
        @ApiParam(value = "查询参数详情", required = true)
        @Valid
        DefectQueryReqVO defectQueryReqVO,
        @ApiParam(value = "页数")
        @QueryParam(value = "pageNum")
        int pageNum,
        @ApiParam(value = "页面大小")
        @QueryParam(value = "pageSize")
        int pageSize,
        @ApiParam(value = "排序字段")
        @QueryParam(value = "sortField")
        String sortField,
        @ApiParam(value = "排序方式")
        @QueryParam(value = "sortType")
        Sort.Direction sortType
    );
}
