package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * 实际执行工具集保存接口
 *
 * @version V2.0
 * @date 2020/11/2
 */
@Tag(name = "BUILD_CHECKER", description = "工具执行记录接口")
@Path("/build/taskLogOverview/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildTaskLogOverviewResource {
    @Operation(summary = "保存工具记录")
    @Path("/saveTools")
    @POST
    Result<Boolean> saveActualTools(
            @Parameter(description = "规则导入请求对象", required = true)
                    TaskLogOverviewVO taskLogOverviewVO
    );

    @Operation(summary = "上报插件错误类型")
    @Path("/reportPluginResult")
    @POST
    Result<Boolean> reportPluginResult(
            @Parameter(description = "请求体", required = true)
            TaskLogOverviewVO taskLogOverviewVO
    );
}
