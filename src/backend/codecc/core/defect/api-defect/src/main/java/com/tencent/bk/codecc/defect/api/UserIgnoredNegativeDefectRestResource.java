package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.IgnoredNegativeDefectStatisticVO;
import com.tencent.bk.codecc.defect.vo.IgnoredNegativeDefectVO;
import com.tencent.bk.codecc.defect.vo.ListNegativeDefectReqVO;
import com.tencent.bk.codecc.defect.vo.OptionalInfoVO;
import com.tencent.bk.codecc.defect.vo.ProcessNegativeDefectReqVO;
import com.tencent.bk.codecc.defect.vo.QueryDefectFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 以 "误报" 为理由被忽略的告警-用户接口
 */
@Api(tags = {"USER_IGNORED_WRONG"}, description = "以误报为理由被忽略的告警")
@Path("/user/ignored/negative/defect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserIgnoredNegativeDefectRestResource {

    @ApiOperation("获取告警对应的代码段")
    @Path("/defectFileContentSegment/toolName/{toolName}")
    @POST
    Result<CommonDefectDetailQueryRspVO> queryDefectFileContentSegment(
            @ApiParam(value = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @ApiParam(value = "获取文件片段相关参数", required = true)
            @Valid
            QueryDefectFileContentSegmentReqVO request
    );

    @ApiOperation("获取告警详情")
    @Path("/withoutFileContent/detail/toolName/{toolName}")
    @POST
    Result<CommonDefectDetailQueryRspVO> queryDefectDetailWithoutFileContent(
            @ApiParam(value = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @ApiParam(value = "查询参数详情", required = true)
            @Valid
            CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO
    );

    @ApiOperation("处理误报告警")
    @Path("/process/defect/toolName/{toolName}/entityId/{entityId}")
    @POST
    Result<Boolean> processNegativeDefect(
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "告警 id", required = true)
            @PathParam("entityId")
            String entityId,
            @ApiParam(value = "请求参数", required = true)
            ProcessNegativeDefectReqVO processNegativeDefectReq
    );

    @ApiOperation("返回过滤后的误报告警列表")
    @Path("/list/defect/toolName/{toolName}/last/{n}")
    @POST
    Result<List<IgnoredNegativeDefectVO>> listDefect(
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @ApiParam(value = "最近 n 天", required = true)
            @PathParam("n")
            Integer n,
            @ApiParam(value = "游标")
            @QueryParam(value = "lastInd")
            String lastInd,
            @ApiParam(value = "Page Size")
            @QueryParam(value = "pageSize")
            Integer pageSize,
            @ApiParam(value = "Order By")
            @QueryParam(value = "sortField")
            String orderBy,
            @ApiParam(value = "排序方向")
            @QueryParam(value = "sortType")
            String orderDirection,
            @ApiParam(value = "请求参数", required = true)
            ListNegativeDefectReqVO listNegativeDefectReq
    );

    @ApiOperation("返回过滤后的误报告警个数")
    @Path("/count/defect/toolName/{toolName}/last/{n}")
    @POST
    Result<Long> countDefectAfterFilter(
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @ApiParam(value = "最近 n 天", required = true)
            @PathParam("n")
            Integer n,
            @ApiParam(value = "请求参数", required = true)
            ListNegativeDefectReqVO listNegativeDefectReq
    );

    @ApiOperation("查询规则, 规则发布者, 规则标签这 3 个筛选条件的可选列表")
    @Path("/list/optional/toolName/{toolName}")
    @GET
    Result<OptionalInfoVO> listOptionalByToolName(
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
            String toolName
    );

    @ApiOperation("查询最近 n 天的特定工具的统计数据")
    @Path("/statistic/toolName/{toolName}/last/{n}")
    @GET
    Result<IgnoredNegativeDefectStatisticVO> statistic(
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @ApiParam(value = "最近 n 天", required = true)
            @PathParam("n")
            Integer n
    );

}
