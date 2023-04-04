package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.dao.mongotemplate.I18NMessageDao;
import com.tencent.bk.codecc.task.model.I18NMessageEntity;
import com.tencent.bk.codecc.task.pojo.I18NQueryModel;
import com.tencent.bk.codecc.task.service.I18NService;
import com.tencent.bk.codecc.task.vo.I18NMessageRequest;
import com.tencent.bk.codecc.task.vo.I18NMessageResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class I18NServiceImpl implements I18NService {

    @Autowired
    private I18NMessageDao i18NMessageDao;

    @Override
    public I18NMessageResponse queryByCondition(I18NMessageRequest request) {
        List<I18NQueryModel> queryModelList = request.stream()
                .map(vo -> new I18NQueryModel(vo.getModuleCode(), vo.getKeySet(), vo.getLocale()))
                .collect(Collectors.toList());

        List<I18NMessageEntity> entityList = i18NMessageDao.query(queryModelList);
        List<I18NMessageResponse.BaseVO> voList = entityList.stream()
                .map(entity -> {
                    I18NMessageResponse.BaseVO target = new I18NMessageResponse.BaseVO();
                    BeanUtils.copyProperties(entity, target);
                    return target;
                }).collect(Collectors.toList());

        I18NMessageResponse response = new I18NMessageResponse();
        response.addAll(voList);

        return response;
    }
}
