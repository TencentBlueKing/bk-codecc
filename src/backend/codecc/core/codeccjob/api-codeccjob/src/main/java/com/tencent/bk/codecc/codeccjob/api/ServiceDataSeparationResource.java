package com.tencent.bk.codecc.codeccjob.api;

import com.tencent.bk.codecc.codeccjob.vo.UpsertPurgingLogRequest;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/service/dataSeparation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceDataSeparationResource {

    @Operation(summary = "更新")
    @Path("/upsertPurgingLog")
    @POST
    Result<Boolean> upsertPurgingLog(
            UpsertPurgingLogRequest request
    );
}
