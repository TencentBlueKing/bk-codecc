package com.tencent.bk.codecc.codeccjob.api;

import com.tencent.bk.codecc.codeccjob.vo.UpsertPurgingLogRequest;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/service/dataSeparation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceDataSeparationResource {

    @ApiOperation("更新")
    @Path("/upsertPurgingLog")
    @POST
    Result<Boolean> upsertPurgingLog(
            UpsertPurgingLogRequest request
    );
}
