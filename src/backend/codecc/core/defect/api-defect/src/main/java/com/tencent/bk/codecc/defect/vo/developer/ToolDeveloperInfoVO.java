package com.tencent.bk.codecc.defect.vo.developer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 工具开发者信息 VO
 *
 * @date 2024/08/08
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("工具开发者信息VO")
public class ToolDeveloperInfoVO {
    @ApiModelProperty("工具名")
    private String toolName;
    @ApiModelProperty("开发者集合")
    private Set<String> developers;
    @ApiModelProperty("拥有者集合")
    private Set<String> owners;
    @ApiModelProperty("管理员集合")
    private Set<String> masters;
}
