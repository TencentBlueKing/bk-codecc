package com.tencent.bk.codecc.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.devops.common.constant.ComConstants;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
public class CheckerStatisticEntity {

    private String id;

    private String name;

    @Field("lang_stats")
    @JsonProperty("lang_stats")
    private List<CheckerStatisticLanguage> langStats;

    @Field("defect_count")
    @JsonProperty("defect_count")
    private int defectCount;

    // 1=>严重，2=>一般，3=>提示
    private int severity;

    @Data
    public static class CheckerStatisticLanguage {
        private String language;

        @Field("lang_value")
        @JsonProperty("lang_value")
        private Long langValue;

        @Field("defect_count")
        @JsonProperty("defect_count")
        private int defectCount;


        public static CheckerStatisticLanguage newInstance(Long langValue, String language) {
            CheckerStatisticLanguage langStat = new CheckerStatisticLanguage();
            langStat.setLangValue(langValue);
            String lang = ComConstants.CodeLang.getCodeLang(langValue);
            langStat.setLanguage(lang != null ? lang : language);
            langStat.setDefectCount(0);
            return langStat;
        }

        public void incDefectCount() {
            this.defectCount += 1;
        }
    }

}
