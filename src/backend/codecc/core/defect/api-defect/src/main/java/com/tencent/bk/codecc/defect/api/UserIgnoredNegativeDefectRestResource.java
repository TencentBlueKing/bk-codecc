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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 以 "误报" 为理由被忽略的告警-用户接口
 */
@Tag(name = "USER_IGNORED_WRONG", description = "以误报为理由被忽略的告警")
@Path("/user/ignored/negative/defect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserIgnoredNegativeDefectRestResource {

    @Operation(summary = "获取告警对应的代码段")
    @Path("/defectFileContentSegment/toolName/{toolName}")
    @POST
    Result<CommonDefectDetailQueryRspVO> queryDefectFileContentSegment(
            @Parameter(description = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @Parameter(description = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @Parameter(description = "获取文件片段相关参数", required = true)
            @Valid
            QueryDefectFileContentSegmentReqVO request
    );

    @Operation(summary = "获取告警详情")
    @Path("/withoutFileContent/detail/toolName/{toolName}")
    @POST
    Result<CommonDefectDetailQueryRspVO> queryDefectDetailWithoutFileContent(
            @Parameter(description = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
            @Parameter(description = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @Parameter(description = "查询参数详情", required = true)
            @Valid
            CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO
    );

    @Operation(summary = "处理误报告警")
    @Path("/process/defect/toolName/{toolName}/entityId/{entityId}")
    @POST
    Result<Boolean> processNegativeDefect(
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "告警 id", required = true)
            @PathParam("entityId")
            String entityId,
            @Parameter(description = "请求参数", required = true)
            ProcessNegativeDefectReqVO processNegativeDefectReq
    );

    @Operation(summary = "返回过滤后的误报告警列表")
    @Path("/list/defect/toolName/{toolName}/last/{n}")
    @POST
    Result<List<IgnoredNegativeDefectVO>> listDefect(
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @Parameter(description = "最近 n 天", required = true)
            @PathParam("n")
            Integer n,
            @Parameter(description = "游标")
            @QueryParam(value = "lastInd")
            String lastInd,
            @Parameter(description = "Page Size")
            @QueryParam(value = "pageSize")
            Integer pageSize,
            @Parameter(description = "Order By")
            @QueryParam(value = "sortField")
            String orderBy,
            @Parameter(description = "排序方向")
            @QueryParam(value = "sortType")
            String orderDirection,
            @Parameter(description = "请求参数", required = true)
            ListNegativeDefectReqVO listNegativeDefectReq
    );

    @Operation(summary = "返回过滤后的误报告警个数")
    @Path("/count/defect/toolName/{toolName}/last/{n}")
    @POST
    Result<Long> countDefectAfterFilter(
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @Parameter(description = "最近 n 天", required = true)
            @PathParam("n")
            Integer n,
            @Parameter(description = "请求参数", required = true)
            ListNegativeDefectReqVO listNegativeDefectReq
    );

    @Operation(summary = "查询规则, 规则发布者, 规则标签这 3 个筛选条件的可选列表")
    @Path("/list/optional/toolName/{toolName}")
    @GET
    Result<OptionalInfoVO> listOptionalByToolName(
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "工具名", required = true)
            @PathParam("toolName")
            String toolName
    );

    @Operation(summary = "查询最近 n 天的特定工具的统计数据")
    @Path("/statistic/toolName/{toolName}/last/{n}")
    @GET
    Result<IgnoredNegativeDefectStatisticVO> statistic(
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @Parameter(description = "最近 n 天", required = true)
            @PathParam("n")
            Integer n
    );

}
