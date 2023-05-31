package com.tencent.bk.codecc.defect.vo;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DefectAuthorGroupStatisticVO {

    public String getAuthorName() {
        if (authorObj instanceof List) {
            return String.join(",", (List) authorObj);
        } else if (authorObj instanceof String) {
            return (String) authorObj;
        } else {
            return "";
        }
    }

    @Getter(AccessLevel.NONE)
    private Object authorObj;
    private int defectCount;
}
