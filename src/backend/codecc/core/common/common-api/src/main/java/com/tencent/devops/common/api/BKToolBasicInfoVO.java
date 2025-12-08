package com.tencent.devops.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "工具基本信息视图")
public class BKToolBasicInfoVO extends ToolMetaBaseVO {
    @Schema(description = "工具开发语言")
    private String devLanguage;
    @Schema(description = "工具类别中文版")
    private List<String> toolCnTypes;
    @Schema(description = "适用语言列表")
    private List<String> langList;
    @Schema(description = "是否需要提供编译脚本")
    private Boolean needBuildScript;
    @Schema(description = "规则数")
    private Long checkerNum;
    @Schema(description = "工具简介，一句话介绍语", required = true)
    private String briefIntroduction;
    @Schema(description = "工具描述，较详细的描述")
    private String description;

    public BKToolBasicInfoVO(String name) {
        this.setName(name);
        this.checkerNum = 0L;
    }
}
