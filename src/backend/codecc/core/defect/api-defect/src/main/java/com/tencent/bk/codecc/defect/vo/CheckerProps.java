package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规则參數配置
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "规则參數配置")
public class CheckerProps {

    @Schema(description = "参数名称")
    private String propName;

    @Schema(description = "参数值")
    private String propValue;

    @Schema(description = "显示值")
    private String displayValue;

}
