package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.itsm.ItsmSystemInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * 获取ITSM系统信息
 */
@Tag(name = "SERVICE_ITSM_INFO", description = "获取ITSM系统信息")
@Path("/service/itsm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceItsmSystemInfoResource {

    @Operation(summary = "查询构建ID关系")
    @Path("/system/{system}")
    @GET
    Result<ItsmSystemInfoVO> getSystemInfo(
            @Parameter(description = "系统")
            @PathParam("system")
            String system,
            @Parameter(description = "版本")
            @QueryParam("version")
            Integer version
    );
}
