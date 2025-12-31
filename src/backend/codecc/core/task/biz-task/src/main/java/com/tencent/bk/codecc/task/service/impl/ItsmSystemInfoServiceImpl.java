package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.dao.mongorepository.ItsmSystemInfoRepository;
import com.tencent.bk.codecc.task.model.istm.ItsmSystemInfoEntity;
import com.tencent.bk.codecc.task.service.ItsmSystemInfoService;
import com.tencent.bk.codecc.task.vo.itsm.ItsmSystemInfoVO;
import com.tencent.devops.common.util.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ItsmSystemInfoServiceImpl implements ItsmSystemInfoService {

    @Autowired
    private ItsmSystemInfoRepository itsmSystemInfoRepository;

    @Override
    public ItsmSystemInfoVO getSystemInfo(String system, Integer version) {
        if (StringUtils.isBlank(system)) {
            return null;
        }
        ItsmSystemInfoEntity itsmSystemInfo;
        if (version != null) {
            itsmSystemInfo = itsmSystemInfoRepository.findFirstBySystemAndVersion(system, version);
        } else {
            itsmSystemInfo = itsmSystemInfoRepository.findFirstBySystem(system);
        }
        if (itsmSystemInfo == null) {
            return null;
        }
        ItsmSystemInfoVO vo = new ItsmSystemInfoVO();
        BeanUtils.copyProperties(itsmSystemInfo, vo);
        return vo;
    }
}
