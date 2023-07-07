package com.tencent.bk.codecc.defect.api;

import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;

@Api(tags = {"SERVICE_CCN_DEFECT"}, description = "ccn告警")
@Path("/service/ccn/defect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceCCNDefectRestResource {
    @ApiOperation("生成id")
    @Path("/genId")
    @POST
    Result<Map<Long, Integer>> genId(
        Set<Long> taskIdSet
    );

}
