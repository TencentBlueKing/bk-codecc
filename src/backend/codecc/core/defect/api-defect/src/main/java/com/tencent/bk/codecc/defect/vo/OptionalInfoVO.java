package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 对于特定工具, 规则, 规则发布者, 规则标签这 3 个筛选条件的可选列表
 *
 * @date 2024/01/16
 */
@Data
@Schema(description = "对于特定工具, 规则, 规则发布者, 规则标签这 3 个筛选条件的可选列表")
public class OptionalInfoVO {

    @Schema(description = "工具名")
    private String toolName;

    @Schema(description = "可选规则列表")
    private List<String> checkers;

    @Schema(description = "可选规则发布者列表")
    private List<String> publishers;

    @Schema(description = "可选规则标签列表")
    private List<String> tags;
}
