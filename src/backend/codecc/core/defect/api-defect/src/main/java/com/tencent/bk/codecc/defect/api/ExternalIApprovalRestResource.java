package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.ignore.ItsmCallbackReqVO;
import com.tencent.bk.codecc.defect.vo.ignore.ItsmCallbackRespVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "EXTERNAL_APPROVAL", description = "ITSM忽略审核回调")
@Path("/external/approval")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ExternalIApprovalRestResource {
        @Path("/callback")
    @POST
    ItsmCallbackRespVO callback(
            @QueryParam("approvalId")
            String approvalId,
            @Parameter(description = "ISTM 回调返回对象", required = true)
            ItsmCallbackReqVO reqVO
    );
}
