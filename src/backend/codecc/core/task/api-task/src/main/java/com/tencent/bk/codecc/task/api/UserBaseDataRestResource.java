package com.tencent.bk.codecc.task.api;

import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TENANT_ID;

/**
 * 基础数据接口类
 *
 * @version V1.0
 * @date 2021/8/17
 */
@Tag(name = "BASE", description = "基础数据查询")
@Path("/user/base")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserBaseDataRestResource {

    @Operation(summary = "根据参数代码获取数据")
    @Path("/paramCode/{paramCode}")
    @GET
    Result<BaseDataVO> getBaseDataByCode(
            @Parameter(description = "参数代码", required = true)
            @PathParam(value = "paramCode")
                    String paramCode);

    @Operation(summary = "获取租户 id")
    @Path("/tenantId")
    @GET
    Result<String> getTenantId(
            @HeaderParam(AUTH_HEADER_DEVOPS_TENANT_ID)
            String tenantId
    );
}
