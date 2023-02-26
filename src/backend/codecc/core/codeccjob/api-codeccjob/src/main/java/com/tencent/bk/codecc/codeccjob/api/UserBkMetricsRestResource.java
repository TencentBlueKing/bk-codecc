package com.tencent.bk.codecc.codeccjob.api;

import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 蓝盾度量数据统计
 */
@Api(tags = {"METRICS"}, value = "度量")
@Path("/user/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserBkMetricsRestResource {

    @ApiOperation("触发统计")
    @Path("/bkMetricsDaily")
    @GET
    Result<Boolean> triggerBkMetrics(
            @ApiParam(value = "统计时间,格式yyyy-MM-dd")
            @QueryParam(value = "statisticsTime")
                    String statisticsTime,
            @ApiParam(value = "项目ID")
            @QueryParam(value = "projectId")
                    String projectId,
            @ApiParam(value = "代码库扫描平均分，精确二位小数")
            @QueryParam(value = "repoCodeccAvgScore")
                    Double repoCodeccAvgScore,
            @ApiParam(value = "已解决缺陷数量")
            @QueryParam(value = "resolvedDefectNum")
                    Integer resolvedDefectNum,
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId
    );
}
