package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户角色 VO
 *
 * @date 2024/05/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("用户角色 VO")
public class UserRoleVO {
    @ApiModelProperty("角色名")
    private String name;
    @ApiModelProperty("角色 id")
    private Integer id;
}
