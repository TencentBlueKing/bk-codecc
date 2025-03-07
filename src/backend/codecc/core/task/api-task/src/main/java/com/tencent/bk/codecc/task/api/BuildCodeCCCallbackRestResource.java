package com.tencent.bk.codecc.task.api;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.task.vo.CodeCCCallbackRegisterVO;
import com.tencent.bk.codecc.task.vo.GrayReportVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants.CodeCCCallbackEvent;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api(tags = {"SERVICE_CODECC_CALLBACK"}, description = "CODECC 回调接口")
@Path("/build/codecc/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildCodeCCCallbackRestResource {

    @ApiOperation("CodeCC回调注册请求")
    @Path("/task/register")
    @POST
    Result<Boolean> registerTaskEvent(
            @ApiParam(value = "CodeCC回调注册请求实体", required = true)
            CodeCCCallbackRegisterVO registerVO,
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );
}
