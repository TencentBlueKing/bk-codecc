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

import com.tencent.bk.codecc.defect.vo.ToolBuildInfoReqVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectRspVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectRspVO;
import com.tencent.bk.codecc.defect.vo.admin.SmokeCheckDetailVO;
import com.tencent.bk.codecc.defect.vo.admin.SmokeCheckLogVO;
import com.tencent.bk.codecc.defect.vo.admin.SmokeCheckReqVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeSysVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.checkerset.CheckerSetParamsVO;
import com.tencent.devops.common.api.checkerset.V3UpdateCheckerSetReqExtVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.domain.Sort;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * op接口资源
 *
 * @date 2020/3/11
 * @version V1.0
 */
@Tag(name = "OP_WARN", description = "告警查询服务接口")
@Path("/op/warn")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OpDefectRestResource {

    @Operation(summary = "运营数据:按条件获取任务告警统计信息")
    @Path("/deptTaskDefect")
    @POST
    Result<DeptTaskDefectRspVO> queryDeptTaskDefect(
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @Parameter(description = "按组织架构查询任务告警请求", required = true)
            @Valid
                    DeptTaskDefectReqVO deptTaskDefectReqVO
    );

    @Operation(summary = "按条件批量获取告警信息列表")
    @Path("/defectInfo/list")
    @POST
    Result<ToolDefectRspVO> queryDeptDefectList(
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @Parameter(description = "按组织架构查询任务告警请求", required = true)
            @Valid
                    DeptTaskDefectReqVO deptTaskDefectReqVO,
            @Parameter(description = "页数")
            @QueryParam(value = "pageNum")
                    Integer pageNum,
            @Parameter(description = "页面大小")
            @QueryParam(value = "pageSize")
                    Integer pageSize,
            @Parameter(description = "排序字段")
            @QueryParam(value = "sortField")
                    String sortField,
            @Parameter(description = "排序方式")
            @QueryParam(value = "sortType")
                    Sort.Direction sortType
    );

    @Operation(summary = "通过分析记录查询时间范围内的活跃项目")
    @Path("/activeTask/list")
    @POST
    Result<DeptTaskDefectRspVO> queryActiveTaskListByLog(
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @Parameter(description = "按组织架构查询任务告警请求", required = true)
            @Valid
                    DeptTaskDefectReqVO deptTaskDefectReqVO
    );

    @Operation(summary = "仅用于刷一次规则描述、详细说明数据")
    @Path("/initCheckerDetailScript")
    @GET
    Result<Boolean> initCheckerDetailScript(@Parameter(description = "工具名") @QueryParam(value = "toolName") String toolName,
            @Parameter(description = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @Parameter(description = "页面大小") @QueryParam(value = "pageSize") Integer pageSize,
            @Parameter(description = "排序字段") @QueryParam(value = "sortField") String sortField,
            @Parameter(description = "排序方式") @QueryParam(value = "sortType") String sortType);


    @Operation(summary = "按任务ID列表代码行数")
    @Path("/codeLine/count")
    @POST
    Result<Long> getTaskCodeLineCount(@Parameter(description = "请求体") @Valid QueryTaskListReqVO reqVO);

    @Operation(summary = "更新规则集基础信息")
    @Path("/checkerSet/update")
    @PUT
    Result<Boolean> updateCheckerSetBaseInfo(
            @Parameter(description = "用户名", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @Parameter(description = "配置规则包参数", required = true) V3UpdateCheckerSetReqExtVO updateCheckerSetReqExtVO
    );

    @Operation(summary = "获取规则集管理初始化参数选项")
    @Path("/params")
    @GET
    Result<CheckerSetParamsVO> getCheckerSetParams();

    @Operation(summary = "初始化代码仓库及分支名数据")
    @Path("/initCodeRepoStatistic")
    @POST
    Result<Boolean> initCodeRepoStatistic(@Parameter(description = "请求体") @Valid DeptTaskDefectReqVO reqVO,
            @Parameter(description = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @Parameter(description = "页面大小") @QueryParam(value = "pageSize") Integer pageSize,
            @Parameter(description = "排序字段") @QueryParam(value = "sortField") String sortField,
            @Parameter(description = "排序方式") @QueryParam(value = "sortType") String sortType);

    @Operation(summary = "修复代码仓库分支总表数据")
    @Path("/codeRepoStatistic/fixed")
    @POST
    Result<Boolean> codeRepoStatisticFixed(
            @Parameter(description = "请求体") @Valid DeptTaskDefectReqVO reqVO
    );

    @Operation(summary = "定时任务及初始化代码库/代码分支数数据")
    @Path("/initCodeRepoStatTrend")
    @POST
    Result<Boolean> initCodeRepoStatTrend(@Parameter(description = "请求体") @Valid QueryTaskListReqVO reqVO);

    @Operation(summary = "查询分析成功的任务和工具信息")
    @Path("/queryAccTaskAndToolName")
    @POST
    Result<List<QueryTaskListReqVO>> queryAccessedTaskAndToolName(
            @Parameter(description = "请求体") @Valid QueryTaskListReqVO reqVO);

    @Operation(summary = "编辑单条工具构建信息")
    @Path("/editOneToolBuildInfo")
    @PUT
    Result<Boolean> editOneToolBuildInfo(
            @Parameter(description = "请求体") @Valid ToolBuildInfoReqVO reqVO,
            @Parameter(description = "用户名", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName
    );

    @Operation(summary = "批量编辑工具构建信息")
    @Path("/editToolBuildInfo")
    @PUT
    Result<Boolean> editToolBuildInfo(
            @Parameter(description = "请求体") @Valid ToolBuildInfoReqVO reqVO,
            @Parameter(description = "用户名", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName
    );

    @Operation(summary = "根据规则集id获取规则集名称")
    @Path("/checker/name")
    @GET
    Result<String> queryCheckerSetNameByCheckerSetId(
            @Parameter(description = "规则集id", required = true) @QueryParam("checkerSetId") String checkerSetId
    );

    @Operation(summary = "新增/修改系统默认的告警忽略类型")
    @Path("/ignoreTypeSysUpdate")
    @POST
    Result<Boolean> ignoreTypeSysUpdate(
            @Parameter(description = "用户名", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @Parameter(description = "冒烟检查请求体", required = true) IgnoreTypeSysVO reqVO
    );

    @Operation(summary = "触发忽略问题Review通知")
    @Path("/trigger/ignoreTypeNotify")
    @GET
    Result<Boolean> triggerProjectStatisticAndSend(
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @Parameter(description = "项目id", required = true)
            @QueryParam("projectId")
                    String projectId,
            @Parameter(description = "忽略类型名", required = true)
            @QueryParam("ignoreTypeName")
                    String ignoreTypeName,
            @Parameter(description = "忽略类型id", required = true)
            @QueryParam("ignoreTypeId")
                    Integer ignoreTypeId,
            @Parameter(description = "忽略类型来源")
            @QueryParam("createFrom")
                    String createFrom
    );
}
