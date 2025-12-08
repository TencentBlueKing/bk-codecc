package com.tencent.bk.codecc.task.api;

import com.tencent.devops.common.api.DynamicConfigVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * 动态配置
 *
 */
@Tag(name = "SERVICE_DYNAMIC_CONFIG", description = "动态配置服务接口")
@Path("/service/dynamic/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceDynamicConfigResource {


    @Operation(summary = "查询构建ID关系")
    @Path("/key/{key}")
    @GET
    Result<DynamicConfigVO> getConfigByKey(@Parameter(description = "键")
                                                     @PathParam("key")
                                                             String key);
}
