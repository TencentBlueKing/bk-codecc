package com.tencent.bk.codecc.defect.api;

import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;

@Tag(name = "SERVICE_CCN_DEFECT", description = "ccn告警")
@Path("/service/ccn/defect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceCCNDefectRestResource {
    @Operation(summary = "生成id")
    @Path("/genId")
    @POST
    Result<Map<Long, Integer>> genId(
        Set<Long> taskIdSet
    );

}
