package com.tencent.bk.codecc.codeccjob.resource;

import com.tencent.bk.codecc.codeccjob.api.ServiceDataSeparationResource;
import com.tencent.bk.codecc.codeccjob.service.DataSeparationService;
import com.tencent.bk.codecc.codeccjob.vo.UpsertPurgingLogRequest;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

@RestResource
public class ServiceDataSeparationResourceImpl implements ServiceDataSeparationResource {

    @Autowired
    private DataSeparationService dataSeparationService;

    @Override
    public Result<Boolean> upsertPurgingLog(UpsertPurgingLogRequest request) {
        dataSeparationService.upsertPurgingLog(
                request.getTaskId(),
                request.getDelCount(),
                request.getCost(),
                request.getFinalResult()
        );

        return new Result<>(true);
    }
}
