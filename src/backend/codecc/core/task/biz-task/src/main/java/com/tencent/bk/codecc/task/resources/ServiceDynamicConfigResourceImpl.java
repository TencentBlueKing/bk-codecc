package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.ServiceDynamicConfigResource;
import com.tencent.bk.codecc.task.dao.mongorepository.DynamicConfigRepository;
import com.tencent.bk.codecc.task.model.DynamicConfigEntity;
import com.tencent.devops.common.api.DynamicConfigVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

@RestResource
public class ServiceDynamicConfigResourceImpl implements ServiceDynamicConfigResource {

    @Autowired
    private DynamicConfigRepository dynamicConfigRepository;

    @Override
    public Result<DynamicConfigVO> getConfigByKey(String key) {
        DynamicConfigEntity entity = dynamicConfigRepository.findFirstByKey(key);
        DynamicConfigVO vo = new DynamicConfigVO();
        if (entity != null) {
            BeanUtils.copyProperties(entity, vo);
        }
        return new Result<>(vo);
    }
}
