package com.tencent.devops.common.service.aop;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class I18NReflection {

    private List<FieldMetaData> fieldMetaDataList;
    private MethodAccess methodAccess;

    public I18NReflection getClone() {
        List<FieldMetaData> cloneFieldMetaDataList = Lists.newArrayList();

        if (this.fieldMetaDataList != null) {
            for (FieldMetaData fieldMetaData : this.fieldMetaDataList) {
                cloneFieldMetaDataList.add(
                        new FieldMetaData(
                                fieldMetaData.willTranslateField,
                                fieldMetaData.resourceCodeField,
                                fieldMetaData.moduleCode,
                                null,
                                null
                        )
                );
            }
        }

        I18NReflection clone = new I18NReflection();
        clone.setMethodAccess(methodAccess);
        clone.setFieldMetaDataList(cloneFieldMetaDataList);

        return clone;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldMetaData {

        private String willTranslateField;
        private String resourceCodeField;
        private String moduleCode;
        private Set<String> keySet;
        private Map<String, String> keyAndValueMap;
    }
}
