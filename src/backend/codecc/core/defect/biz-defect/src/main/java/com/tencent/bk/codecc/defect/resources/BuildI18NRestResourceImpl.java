package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildI18NRestResource;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.service.utils.I18NUtils;
import com.tencent.devops.common.web.RestResource;

@RestResource
public class BuildI18NRestResourceImpl implements BuildI18NRestResource {

    @Override
    public Result<String> getLanguageTag(String userId) {
        try {
            // todo:等开源版api-project版本号
            return new Result<>(I18NUtils.CN.toLanguageTag());
        } catch (Throwable t) {
            return new Result<>(I18NUtils.CN.toLanguageTag());
        }
    }
}
