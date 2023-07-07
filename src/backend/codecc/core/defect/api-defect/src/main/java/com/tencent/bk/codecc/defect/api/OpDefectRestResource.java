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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.data.domain.Sort;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * op接口资源
 *
 * @date 2020/3/11
 * @version V1.0
 */
@Api(tags = {"OP_WARN"}, value = "告警查询服务接口")
@Path("/op/warn")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OpDefectRestResource {

    @ApiOperation("运营数据:按条件获取任务告警统计信息")
    @Path("/deptTaskDefect")
    @POST
    Result<DeptTaskDefectRspVO> queryDeptTaskDefect(
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @ApiParam(value = "按组织架构查询任务告警请求", required = true)
            @Valid
                    DeptTaskDefectReqVO deptTaskDefectReqVO
    );

    @ApiOperation("按条件批量获取告警信息列表")
    @Path("/defectInfo/list")
    @POST
    Result<ToolDefectRspVO> queryDeptDefectList(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @ApiParam(value = "按组织架构查询任务告警请求", required = true)
            @Valid
                    DeptTaskDefectReqVO deptTaskDefectReqVO,
            @ApiParam(value = "页数")
            @QueryParam(value = "pageNum")
                    Integer pageNum,
            @ApiParam(value = "页面大小")
            @QueryParam(value = "pageSize")
                    Integer pageSize,
            @ApiParam(value = "排序字段")
            @QueryParam(value = "sortField")
                    String sortField,
            @ApiParam(value = "排序方式")
            @QueryParam(value = "sortType")
                    Sort.Direction sortType
    );

    @ApiOperation("通过分析记录查询时间范围内的活跃项目")
    @Path("/activeTask/list")
    @POST
    Result<DeptTaskDefectRspVO> queryActiveTaskListByLog(
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @ApiParam(value = "按组织架构查询任务告警请求", required = true)
            @Valid
                    DeptTaskDefectReqVO deptTaskDefectReqVO
    );

    @ApiOperation("仅用于刷一次规则描述、详细说明数据")
    @Path("/initCheckerDetailScript")
    @GET
    Result<Boolean> initCheckerDetailScript(@ApiParam(value = "工具名") @QueryParam(value = "toolName") String toolName,
            @ApiParam(value = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @ApiParam(value = "页面大小") @QueryParam(value = "pageSize") Integer pageSize,
            @ApiParam(value = "排序字段") @QueryParam(value = "sortField") String sortField,
            @ApiParam(value = "排序方式") @QueryParam(value = "sortType") String sortType);


    @ApiOperation("按任务ID列表代码行数")
    @Path("/codeLine/count")
    @POST
    Result<Long> getTaskCodeLineCount(@ApiParam(value = "请求体") @Valid QueryTaskListReqVO reqVO);

    @ApiOperation("更新规则集基础信息")
    @Path("/checkerSet/update")
    @PUT
    Result<Boolean> updateCheckerSetBaseInfo(
            @ApiParam(value = "用户名", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @ApiParam(value = "配置规则包参数", required = true) V3UpdateCheckerSetReqExtVO updateCheckerSetReqExtVO
    );

    @ApiOperation("获取规则集管理初始化参数选项")
    @Path("/params")
    @GET
    Result<CheckerSetParamsVO> getCheckerSetParams();

    @ApiOperation("初始化代码仓库及分支名数据")
    @Path("/initCodeRepoStatistic")
    @POST
    Result<Boolean> initCodeRepoStatistic(@ApiParam(value = "请求体") @Valid DeptTaskDefectReqVO reqVO,
            @ApiParam(value = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @ApiParam(value = "页面大小") @QueryParam(value = "pageSize") Integer pageSize,
            @ApiParam(value = "排序字段") @QueryParam(value = "sortField") String sortField,
            @ApiParam(value = "排序方式") @QueryParam(value = "sortType") String sortType);

    @ApiOperation("修复代码仓库分支总表数据")
    @Path("/codeRepoStatistic/fixed")
    @POST
    Result<Boolean> codeRepoStatisticFixed(
            @ApiParam(value = "请求体") @Valid DeptTaskDefectReqVO reqVO
    );

    @ApiOperation("定时任务及初始化代码库/代码分支数数据")
    @Path("/initCodeRepoStatTrend")
    @POST
    Result<Boolean> initCodeRepoStatTrend(@ApiParam(value = "请求体") @Valid QueryTaskListReqVO reqVO);

    @ApiOperation("查询分析成功的任务和工具信息")
    @Path("/queryAccTaskAndToolName")
    @POST
    Result<List<QueryTaskListReqVO>> queryAccessedTaskAndToolName(
            @ApiParam(value = "请求体") @Valid QueryTaskListReqVO reqVO);

    @ApiOperation("编辑单条工具构建信息")
    @Path("/editOneToolBuildInfo")
    @PUT
    Result<Boolean> editOneToolBuildInfo(
            @ApiParam(value = "请求体") @Valid ToolBuildInfoReqVO reqVO,
            @ApiParam(value = "用户名", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName
    );

    @ApiOperation("批量编辑工具构建信息")
    @Path("/editToolBuildInfo")
    @PUT
    Result<Boolean> editToolBuildInfo(
            @ApiParam(value = "请求体") @Valid ToolBuildInfoReqVO reqVO,
            @ApiParam(value = "用户名", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName
    );

    @ApiOperation("根据规则集id获取规则集名称")
    @Path("/checker/name")
    @GET
    Result<String> queryCheckerSetNameByCheckerSetId(
            @ApiParam(value = "规则集id", required = true) @QueryParam("checkerSetId") String checkerSetId
    );

    @ApiOperation("新增/修改系统默认的告警忽略类型")
    @Path("/ignoreTypeSysUpdate")
    @POST
    Result<Boolean> ignoreTypeSysUpdate(
            @ApiParam(value = "用户名", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @ApiParam(value = "冒烟检查请求体", required = true) IgnoreTypeSysVO reqVO
    );

    @ApiOperation("触发忽略问题Review通知")
    @Path("/trigger/ignoreTypeNotify")
    @GET
    Result<Boolean> triggerProjectStatisticAndSend(
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @ApiParam(value = "项目id", required = true)
            @QueryParam("projectId")
                    String projectId,
            @ApiParam(value = "忽略类型名", required = true)
            @QueryParam("ignoreTypeName")
                    String ignoreTypeName,
            @ApiParam(value = "忽略类型id", required = true)
            @QueryParam("ignoreTypeId")
                    Integer ignoreTypeId,
            @ApiParam(value = "忽略类型来源")
            @QueryParam("createFrom")
                    String createFrom
    );
}
