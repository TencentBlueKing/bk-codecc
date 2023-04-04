package com.tencent.bk.codecc.task.aop;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.task.dao.mongotemplate.I18NMessageDao;
import com.tencent.bk.codecc.task.model.I18NMessageEntity;
import com.tencent.bk.codecc.task.pojo.I18NQueryModel;
import com.tencent.devops.common.service.aop.AbstractI18NResponseAspect;
import com.tencent.devops.common.service.aop.I18NReflection;
import com.tencent.devops.common.service.aop.I18NReflection.FieldMetaData;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Aspect
@Component
public class I18NResponseAspect extends AbstractI18NResponseAspect {

    @Autowired
    private I18NMessageDao i18NMessageDao;

    @Override
    public void addInternationalization(I18NReflection i18NReflection, String localeString) {
        if (i18NReflection == null || CollectionUtils.isEmpty(i18NReflection.getFieldMetaDataList())) {
            return;
        }

        List<I18NMessageEntity> i18NMessageList = getI18NMessage(i18NReflection, localeString);
        if (CollectionUtils.isEmpty(i18NMessageList)) {
            return;
        }

        Map<String, List<I18NMessageEntity>> i18NMessageMap = i18NMessageList.stream()
                .collect(Collectors.groupingBy(I18NMessageEntity::getModuleCode));

        for (FieldMetaData fieldMetaData : i18NReflection.getFieldMetaDataList()) {
            List<I18NMessageEntity> i18NMessageEntityList = i18NMessageMap.get(fieldMetaData.getModuleCode());
            if (i18NMessageEntityList == null) {
                continue;
            }

            Map<String, String> kvMap = i18NMessageEntityList.stream()
                    .collect(Collectors.toMap(I18NMessageEntity::getKey, I18NMessageEntity::getValue, (k1, k2) -> k1));
            fieldMetaData.setKeyAndValueMap(kvMap);
        }
    }

    private List<I18NMessageEntity> getI18NMessage(I18NReflection i18NReflection, String localeString) {
        try {
            List<I18NQueryModel> queryModelList = i18NReflection.getFieldMetaDataList().stream()
                    .map(x -> new I18NQueryModel(x.getModuleCode(), x.getKeySet(), localeString))
                    .collect(Collectors.toList());

            return i18NMessageDao.query(queryModelList);
        } catch (Throwable t) {
            log.error("get i18 message from db fail", t);

            return Lists.newArrayList();
        }
    }
}
