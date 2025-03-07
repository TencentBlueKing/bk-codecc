package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.ignore.ItsmCallbackReqVO;
import com.tencent.bk.codecc.defect.vo.ignore.ItsmCallbackRespVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api(tags = {"EXTERNAL_APPROVAL"}, description = "ITSM忽略审核回调")
@Path("/external/approval")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ExternalIApprovalRestResource {
    @ApiOperation("")
    @Path("/callback")
    @POST
    ItsmCallbackRespVO callback(
            @QueryParam("approvalId")
            String approvalId,
            @ApiParam(value = "ISTM 回调返回对象", required = true)
            ItsmCallbackReqVO reqVO
    );
}
