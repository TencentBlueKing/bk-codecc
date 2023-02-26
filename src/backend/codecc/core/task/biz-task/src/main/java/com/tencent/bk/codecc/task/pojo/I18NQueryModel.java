package com.tencent.bk.codecc.task.pojo;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class I18NQueryModel {

    private String moduleCode;

    private Set<String> keySet;

    private String locale;
}
