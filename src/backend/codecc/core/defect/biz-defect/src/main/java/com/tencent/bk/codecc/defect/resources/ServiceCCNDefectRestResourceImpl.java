package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceCCNDefectRestResource;
import com.tencent.bk.codecc.defect.service.CCNDefectService;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;

@RestResource
public class ServiceCCNDefectRestResourceImpl implements ServiceCCNDefectRestResource {

    @Autowired
    private CCNDefectService ccnDefectService;

    @Override
    public Result<Map<Long, Integer>> genId(Set<Long> taskIdSet) {
        return new Result<>(ccnDefectService.genId(taskIdSet));
    }
}
