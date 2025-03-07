package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.ServiceItsmSystemInfoResource;
import com.tencent.bk.codecc.task.service.ItsmSystemInfoService;
import com.tencent.bk.codecc.task.vo.itsm.ItsmSystemInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@RestResource
public class ServiceItsmSystemInfoResourceImpl implements ServiceItsmSystemInfoResource {

    @Autowired
    private ItsmSystemInfoService itsmSystemInfoService;

    @Override
    public Result<ItsmSystemInfoVO> getSystemInfo(String system) {
        return new Result<>(StringUtils.isBlank(system) ? null : itsmSystemInfoService.getSystemInfo(system));
    }
}
