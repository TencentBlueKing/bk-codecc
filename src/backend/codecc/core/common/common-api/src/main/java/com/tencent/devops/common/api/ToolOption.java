package com.tencent.devops.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工具自定义参数
 *
 * @date 2025/02/17
 */
@Data
public class ToolOption {
    @Schema(description = "参数名", required = true)
    private String varName;

    @Schema(description = "参数类型，可选值：NUMBER,STRING,BOOLEAN,RADIO,CHECKBOX，"
            + "分别表示：数字，字符串，布尔值，单选框，复选框", required = true)
    private String varType;

    @Schema(description = "参数展示名，不填展示varName")
    private String labelName;

    @Schema(description = "参数默认值")
    private String varDefault;

    @Schema(description = "参数说明")
    private String varTips;

    @Schema(description = "参数是否必填：true必填，false非必填")
    private boolean varRequired;

    @Schema(description = "varType为RADIO或CHECKBOX时必填，表示单选框或复选框的选项列表")
    private List<VarOption> varOptionList;

    @Schema(description = "该参数的值如果有变化，是否需要让对应的扫描任务做一次全量扫描，默认为 false")
    private boolean fullScanOnChange = false;

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class VarOption {
        @Schema(description = "选项展示名")
        private String name;
        @Schema(description = "选项 id")
        private String id;
    }
}
