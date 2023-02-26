package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceDefectDataResource;
import com.tencent.bk.codecc.defect.service.RefreshDefectBizService;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

@RestResource
@Slf4j
public class ServiceDefectDataResourceImpl implements ServiceDefectDataResource {
    @Autowired
    private RefreshDefectBizService refreshDefectBizService;

    @Override
    public Result<String> freshToolStatic(Set<Long> taskIds) {
        return new Result<>(refreshDefectBizService.freshClocDefectByPage(taskIds));
    }

}
