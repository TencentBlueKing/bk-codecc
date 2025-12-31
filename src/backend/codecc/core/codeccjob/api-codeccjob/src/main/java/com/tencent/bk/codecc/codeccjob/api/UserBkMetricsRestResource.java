package com.tencent.bk.codecc.codeccjob.api;

import com.tencent.devops.common.api.pojo.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 蓝盾度量数据统计
 */
@Tag(name = "METRICS", description = "度量")
@Path("/user/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserBkMetricsRestResource {

    @Operation(summary = "触发统计")
    @Path("/bkMetricsDaily")
    @GET
    Result<Boolean> triggerBkMetrics(
            @Parameter(description = "统计时间,格式yyyy-MM-dd")
            @QueryParam(value = "statisticsTime")
                    String statisticsTime,
            @Parameter(description = "项目ID")
            @QueryParam(value = "projectId")
                    String projectId,
            @Parameter(description = "代码库扫描平均分，精确二位小数")
            @QueryParam(value = "repoCodeccAvgScore")
                    Double repoCodeccAvgScore,
            @Parameter(description = "已解决缺陷数量")
            @QueryParam(value = "resolvedDefectNum")
                    Integer resolvedDefectNum,
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId
    );
}
