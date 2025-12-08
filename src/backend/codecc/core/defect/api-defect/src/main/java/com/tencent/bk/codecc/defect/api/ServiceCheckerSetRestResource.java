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

import com.tencent.bk.codecc.defect.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.checkerset.CheckerSetPackageVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.checkerset.OpenSourceCheckerSetVO;
import com.tencent.devops.common.api.checkerset.*;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 规则集接口
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Tag(name = "SERVICE_CHECKER_SET", description = " 配置规则集接口")
@Path("/service/checkerSet")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceCheckerSetRestResource
{
    @Operation(summary = "任务关联规则集")
    @Path("/project/{projectId}/tasks/{taskId}/checkerSets/relationship")
    @POST
    Result<Boolean> batchRelateTaskAndCheckerSet(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @Parameter(description = "项目Id", required = true)
            @PathParam("projectId")
                    String projectId,
            @Parameter(description = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "任务关联的规则集", required = true)
                    List<CheckerSetVO> checkerSetList,
            @Parameter(description = "是否开源")
            @QueryParam("isOpenSource")
                    Boolean isOpenSource
    );

    @Operation(summary = "根据规则ID列表查询规则集")
    @Path("/project/{projectId}")
    @POST
    Result<List<CheckerSetVO>> queryCheckerSets(
            @Parameter(description = "规则集列表", required = true)
                    Set<String> checkerSetList,
            @Parameter(description = "项目Id", required = true)
            @PathParam("projectId")
                    String projectId);

    @Operation(summary = "根据任务Id查询任务已经关联的规则集列表")
    @Path("/tasks/{taskId}/list")
    @POST
    Result<List<CheckerSetVO>> getCheckerSets(
            @Parameter(description = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId
    );

    @Operation(summary = "查询规则集列表")
    @Path("/list")
    @POST
    Result<List<CheckerSetVO>> getCheckerSets(
        @Parameter(description = "配置规则包参数", required = true)
            CheckerSetListQueryReq queryCheckerSetReq
    );

    @Operation(summary = "根据任务和语言解绑相应的规则集")
    @Path("/task/{taskId}/codeLang/{codeLang}")
    @POST
    Result<Boolean> updateCheckerSetAndTaskRelation(
            @Parameter(description = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "项目语言", required = true)
            @PathParam("codeLang")
                    Long codeLang,
            @Parameter(description = "项目语言", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user);


    @Operation(summary = "获取规则和规则集数量")
    @Path("/count/task/{taskId}/projectId/{projectId}")
    @POST
    Result<TaskBaseVO> getCheckerAndCheckerSetCount(
            @Parameter(description = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "项目Id", required = true)
            @PathParam("projectId")
                    String projectId
    );

    @Operation(summary = "规则集关联到项目或任务")
    @Path("/{checkerSetId}/relationships")
    @POST
    Result<Boolean> setRelationships(
            @Parameter(description = "规则集Id", required = true)
            @PathParam("checkerSetId")
                    String checkerSetId,
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @Parameter(description = "规则集关联到项目或任务", required = true)
                    CheckerSetRelationshipVO checkerSetRelationshipVO
    );

    @Operation(summary = "批量地将多个规则集关联到一个项目或任务")
    @Path("/batchSet/relationships")
    @POST
    Result<Boolean> batchSetRelationships(
            @Parameter(description = "用户 id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String user,
            @Parameter(description = "规则集关联到项目或任务", required = true)
            CheckerSetRelationshipVO checkerSetRelationshipVO
    );

    @Operation(summary = "根据规则ID列表查询规则集")
    @Path("/project/openscan/checkerSet")
    @POST
    Result<List<CheckerSetVO>> queryCheckerSetsForOpenScan(
            @Parameter(description = "规则集列表", required = true)
                    Set<CheckerSetVO> checkerSetList);

    @Operation(summary = "查询规则列表通过指定规则集")
    @Path("/listForContent")
    @POST
    Result<List<CheckerSetVO>> getCheckerSetsForContent(
            @Parameter(description = "规则集列表", required = true)
            List<String> checkerSetIdList
    );

    @Operation(summary = "根据语言获取所有的规则包")
    @Path("/getPackageByLangValue")
    @GET
    Result<List<CheckerSetPackageVO>> getPackageByLangValue(
            @Parameter(description = "语言值")
            @QueryParam("langValue")
            Long langValue
    );
}
