package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规则类型[一中告警类型对应多个告警列表]
 */
@Data
@Schema(description = "CPPLINT和ESLINT告警下拉列表视图")
@AllArgsConstructor
@NoArgsConstructor
public class CheckerCustomVO {

    @Schema(description = "告警类型")
    private String typeName;

    @Schema(description = "告警列表")
    private List<String> checkers;
}
