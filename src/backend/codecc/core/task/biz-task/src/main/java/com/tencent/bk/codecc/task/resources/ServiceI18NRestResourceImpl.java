package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.ServiceI18NRestResource;
import com.tencent.bk.codecc.task.service.I18NService;
import com.tencent.bk.codecc.task.vo.I18NMessageRequest;
import com.tencent.bk.codecc.task.vo.I18NMessageResponse;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@RestResource
public class ServiceI18NRestResourceImpl implements ServiceI18NRestResource {

    @Autowired
    private I18NService i18nService;

    @Override
    public Result<I18NMessageResponse> getI18NMessage(I18NMessageRequest request) {
        log.info("getI18NMessage req: {}", JsonUtil.INSTANCE.toJson(request));
        return new Result<>(i18nService.queryByCondition(request));
    }
}
