package com.tencent.devops.common.api;

import io.swagger.annotations.ApiModelProperty;
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
    @ApiModelProperty(value = "参数名", required = true)
    private String varName;

    @ApiModelProperty(value = "参数类型，可选值：NUMBER,STRING,BOOLEAN,RADIO,CHECKBOX，"
            + "分别表示：数字，字符串，布尔值，单选框，复选框", required = true)
    private String varType;

    @ApiModelProperty(value = "参数展示名，不填展示varName")
    private String labelName;

    @ApiModelProperty(value = "参数默认值")
    private String varDefault;

    @ApiModelProperty(value = "参数说明")
    private String varTips;

    @ApiModelProperty(value = "参数是否必填：true必填，false非必填")
    private boolean varRequired;

    @ApiModelProperty(value = "varType为RADIO或CHECKBOX时必填，表示单选框或复选框的选项列表")
    private List<VarOption> varOptionList;

    @ApiModelProperty(value = "该参数的值如果有变化，是否需要让对应的扫描任务做一次全量扫描，默认为 false")
    private boolean fullScanOnChange = false;

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class VarOption {
        @ApiModelProperty(value = "选项展示名")
        private String name;
        @ApiModelProperty(value = "选项 id")
        private String id;
    }
}
