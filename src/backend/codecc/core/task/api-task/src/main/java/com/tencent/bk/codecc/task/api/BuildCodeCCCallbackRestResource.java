package com.tencent.bk.codecc.task.api;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.task.vo.CodeCCCallbackRegisterVO;
import com.tencent.bk.codecc.task.vo.GrayReportVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants.CodeCCCallbackEvent;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "SERVICE_CODECC_CALLBACK", description = "CODECC 回调接口")
@Path("/build/codecc/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildCodeCCCallbackRestResource {

    @Operation(summary = "CodeCC回调注册请求")
    @Path("/task/register")
    @POST
    Result<Boolean> registerTaskEvent(
            @Parameter(description = "CodeCC回调注册请求实体", required = true)
            CodeCCCallbackRegisterVO registerVO,
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );
}
