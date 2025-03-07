package com.tencent.devops.common.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工具基本信息视图
 *
 * @date 2024/08/07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ApiModel("工具基本信息视图")
public class BKToolBasicInfoVO extends ToolMetaBaseVO {
    @ApiModelProperty("工具开发语言")
    private String devLanguage;
    @ApiModelProperty("工具类别中文版")
    private List<String> toolCnTypes;
    @ApiModelProperty("适用语言列表")
    private List<String> langList;
    @ApiModelProperty("是否需要提供编译脚本")
    private Boolean needBuildScript;
    @ApiModelProperty("规则数")
    private Long checkerNum;
    @ApiModelProperty(value = "工具简介，一句话介绍语", required = true)
    private String briefIntroduction;
    @ApiModelProperty(value = "工具描述，较详细的描述")
    private String description;

    public BKToolBasicInfoVO(String name) {
        this.setName(name);
        this.checkerNum = 0L;
    }
}
