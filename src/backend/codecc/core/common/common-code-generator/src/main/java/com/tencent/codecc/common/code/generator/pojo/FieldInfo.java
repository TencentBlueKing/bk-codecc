package com.tencent.codecc.common.code.generator.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FieldInfo {

    public String databaseFiledName;
    public String javaFieldName;
    public String javaSetterName;
    public String javaFiledType;
    // 基本类型
    public boolean isPrimitive;
}
