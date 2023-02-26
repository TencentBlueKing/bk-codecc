package com.tencent.bk.codecc.defect.aop;

import com.tencent.bk.codecc.task.api.ServiceI18NRestResource;
import com.tencent.bk.codecc.task.vo.I18NMessageRequest;
import com.tencent.bk.codecc.task.vo.I18NMessageResponse;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.service.aop.AbstractI18NResponseAspect;
import com.tencent.devops.common.service.aop.I18NReflection;
import com.tencent.devops.common.service.aop.I18NReflection.FieldMetaData;
import java.util.List;
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
    public void addInternationalization(I18NReflection i18NReflection, String localeString) {
        if (i18NReflection == null || CollectionUtils.isEmpty(i18NReflection.getFieldMetaDataList())) {
            return;
        }

        List<I18NMessageResponse.BaseVO> i18NMessageList = getI18NMessage(i18NReflection, localeString);
        if (CollectionUtils.isEmpty(i18NMessageList)) {
            return;
        }

        Map<String, List<I18NMessageResponse.BaseVO>> i18NMessageMap = i18NMessageList.stream()
                .collect(Collectors.groupingBy(I18NMessageResponse.BaseVO::getModuleCode));

        for (FieldMetaData fieldMetaData : i18NReflection.getFieldMetaDataList()) {
            List<I18NMessageResponse.BaseVO> i18NMessageVOList = i18NMessageMap.get(fieldMetaData.getModuleCode());
            if (i18NMessageVOList == null) {
                continue;
            }

            Map<String, String> kvMap = i18NMessageVOList.stream()
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

    private List<I18NMessageResponse.BaseVO> getI18NMessage(I18NReflection i18NReflection, String localeString) {
        try {
            I18NMessageRequest request = new I18NMessageRequest();
            for (FieldMetaData fieldMetaData : i18NReflection.getFieldMetaDataList()) {
                request.add(
                        new I18NMessageRequest.BaseVO(
                                fieldMetaData.getModuleCode(),
                                fieldMetaData.getKeySet(),
                                localeString
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
