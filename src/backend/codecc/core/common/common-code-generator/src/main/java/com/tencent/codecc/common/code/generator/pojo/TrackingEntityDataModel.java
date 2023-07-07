package com.tencent.codecc.common.code.generator.pojo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackingEntityDataModel {

    private List<FieldInfo> fieldInfoList;
    private String baseEntityName;
    private String baseEntityImport;
    private String fullPackagePath;
    // 字段数量
    private int numberOfFields;
}
