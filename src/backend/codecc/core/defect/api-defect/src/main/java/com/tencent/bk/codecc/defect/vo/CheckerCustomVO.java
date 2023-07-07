package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规则类型[一中告警类型对应多个告警列表]
 */
@Data
@ApiModel("CPPLINT和ESLINT告警下拉列表视图")
@AllArgsConstructor
@NoArgsConstructor
public class CheckerCustomVO {

    @ApiModelProperty("告警类型")
    private String typeName;

    @ApiModelProperty("告警列表")
    private List<String> checkers;
}
