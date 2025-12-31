package com.tencent.bk.codecc.defect.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "按照规则维度查询告警数返回体")
public class CheckerStatisticRspVO {
    private List<CheckerStatisticVO> currentStatisticList;
    private List<CheckerStatisticVO> lastStatisticList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CheckerStatisticVO {

        private String name;

        @JsonProperty("defect_count")
        private int defectCount;
    }
}
