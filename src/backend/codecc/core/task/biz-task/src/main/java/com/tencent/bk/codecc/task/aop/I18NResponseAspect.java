package com.tencent.bk.codecc.task.aop;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.task.dao.mongotemplate.I18NMessageDao;
import com.tencent.bk.codecc.task.model.I18NMessageEntity;
import com.tencent.bk.codecc.task.pojo.I18NQueryModel;
import com.tencent.devops.common.service.aop.AbstractI18NResponseAspect;
import com.tencent.devops.common.service.aop.I18NReflection;
import com.tencent.devops.common.service.aop.I18NReflection.FieldMetaData;
import java.util.List;
import java.util.Locale;
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
    private I18NMessageDao i18nMessageDao;

    @Override
    public void addInternationalization(I18NReflection i18nReflection, Locale locale) {
        if (i18nReflection == null || CollectionUtils.isEmpty(i18nReflection.getFieldMetaDataList())) {
            return;
        }

        // db存储的国际化信息不分地区信息
        List<I18NMessageEntity> i18nMessageList = getI18NMessage(i18nReflection, locale);
        if (CollectionUtils.isEmpty(i18nMessageList)) {
            return;
        }

        Map<String, List<I18NMessageEntity>> i18nMessageMap = i18nMessageList.stream()
                .collect(Collectors.groupingBy(I18NMessageEntity::getModuleCode));

        for (FieldMetaData fieldMetaData : i18nReflection.getFieldMetaDataList()) {
            List<I18NMessageEntity> i18nMessageEntityList = i18nMessageMap.get(fieldMetaData.getModuleCode());
            if (i18nMessageEntityList == null) {
                continue;
            }

            Map<String, String> kvMap = i18nMessageEntityList.stream()
                    .collect(Collectors.toMap(I18NMessageEntity::getKey, I18NMessageEntity::getValue, (k1, k2) -> k1));
            fieldMetaData.setKeyAndValueMap(kvMap);
        }
    }

    private List<I18NMessageEntity> getI18NMessage(I18NReflection i18nReflection, Locale locale) {
        try {
            List<I18NQueryModel> queryModelList = i18nReflection.getFieldMetaDataList().stream()
                    .map(x -> new I18NQueryModel(x.getModuleCode(), x.getKeySet(), locale.toString()))
                    .collect(Collectors.toList());

            return i18nMessageDao.query(queryModelList);
        } catch (Throwable t) {
            log.error("get i18 message from db fail", t);

            return Lists.newArrayList();
        }
    }
}
