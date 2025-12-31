package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.common.BackendParamsVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * 工具开发者信息接口
 *
 * @version V1.0
 * @date 2025/8/6
 */
@Tag(name = "SERVICE_TOOL_DEVELOPER_INFO", description = "工具开发者信息接口")
@Path("/service/toolDeveloperInfo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceToolDeveloperInfoResource {

    @Operation(summary = "初始化工具开发者开发者")
    @Path("/")
    @GET
    Result<Boolean> initializationToolDeveloper();
}
