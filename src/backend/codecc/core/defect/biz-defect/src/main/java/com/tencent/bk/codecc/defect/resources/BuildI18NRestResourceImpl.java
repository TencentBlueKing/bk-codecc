package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildI18NRestResource;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;

@RestResource
public class BuildI18NRestResourceImpl implements BuildI18NRestResource {

    @Override
    public Result<String> getLanguageTag(String userId) {
        return new Result<>("en");
    }
}