package com.tencent.bk.codecc.defect.aop;

import com.tencent.bk.codecc.task.api.ServiceI18NRestResource;
import com.tencent.bk.codecc.task.vo.I18NMessageRequest;
import com.tencent.bk.codecc.task.vo.I18NMessageResponse;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.service.aop.AbstractI18NResponseAspect;
import com.tencent.devops.common.service.aop.I18NReflection;
import com.tencent.devops.common.service.aop.I18NReflection.FieldMetaData;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Aspect
@Component
public class I18NResponseAspect extends AbstractI18NResponseAspect {

    @Autowired
    private Client client;

    @Override
    public void addInternationalization(I18NReflection i18nReflection, Locale locale) {
        if (i18nReflection == null || CollectionUtils.isEmpty(i18nReflection.getFieldMetaDataList())) {
            return;
        }

        List<I18NMessageResponse.BaseVO> i18nMessageList = getI18NMessage(i18nReflection, locale);
        if (CollectionUtils.isEmpty(i18nMessageList)) {
            return;
        }

        Map<String, List<I18NMessageResponse.BaseVO>> i18nMessageMap = i18nMessageList.stream()
                .collect(Collectors.groupingBy(I18NMessageResponse.BaseVO::getModuleCode));

        for (FieldMetaData fieldMetaData : i18nReflection.getFieldMetaDataList()) {
            List<I18NMessageResponse.BaseVO> i18nMessageVOList = i18nMessageMap.get(fieldMetaData.getModuleCode());
            if (i18nMessageVOList == null) {
                continue;
            }

            Map<String, String> kvMap = i18nMessageVOList.stream()
                    .collect(
                            Collectors.toMap(
                                    I18NMessageResponse.BaseVO::getKey,
                                    I18NMessageResponse.BaseVO::getValue,
                                    (k1, k2) -> k1
                            )
                    );
            fieldMetaData.setKeyAndValueMap(kvMap);
        }
    }

    private List<I18NMessageResponse.BaseVO> getI18NMessage(I18NReflection i18nReflection, Locale locale) {
        try {
            I18NMessageRequest request = new I18NMessageRequest();
            for (FieldMetaData fieldMetaData : i18nReflection.getFieldMetaDataList()) {
                request.add(
                        new I18NMessageRequest.BaseVO(
                                fieldMetaData.getModuleCode(),
                                fieldMetaData.getKeySet(),
                                locale.toString()
                        )
                );
            }

            return client.get(ServiceI18NRestResource.class).getI18NMessage(request).getData();
        } catch (Throwable t) {
            log.error("get i18n message from defect service fail", t);

            return Lists.newArrayList();
        }
    }
}
