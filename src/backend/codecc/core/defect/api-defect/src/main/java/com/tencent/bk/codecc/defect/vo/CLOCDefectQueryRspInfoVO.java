package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("代码统计条目信息返回视图")
public class CLOCDefectQueryRspInfoVO {

    @ApiModelProperty("语言名称")
    private String language;

    @ApiModelProperty("当前语言行数总和")
    private long sumLines;

    @ApiModelProperty("当前语言空白行数")
    private long sumBlank;

    @ApiModelProperty("当前语言代码行数")
    private long sumCode;

    @ApiModelProperty("当前语言注释行数")
    private long sumComment;

    @ApiModelProperty("注释率")
    private Double commentRate;

    @ApiModelProperty("当前语言有效注释行数")
    private Long sumEfficientComment;

    @ApiModelProperty("有效注释率")
    private Double efficientCommentRate;

    @ApiModelProperty("当前语言行数占比")
    private int proportion;

    @ApiModelProperty("项目总文件数")
    private long sumFileNum;
}
