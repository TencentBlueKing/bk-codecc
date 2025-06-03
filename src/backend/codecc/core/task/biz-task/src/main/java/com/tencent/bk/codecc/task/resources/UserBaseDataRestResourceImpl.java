package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.UserBaseDataRestResource;
import com.tencent.bk.codecc.task.service.BaseDataService;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 基础数据接口的实现类
 *
 * @version V1.0
 * @date 2021/8/17
 */
@RestResource
public class UserBaseDataRestResourceImpl implements UserBaseDataRestResource {

    @Autowired
    private BaseDataService baseDataService;

    @Override
    public Result<BaseDataVO> getBaseDataByCode(String paramCode) {
        return new Result<>(baseDataService.findBaseDataByCode(paramCode));
    }

    @Override
    public Result<String>  getTenantId(String tenantId) {
        return new Result<>(tenantId);
    }
}
