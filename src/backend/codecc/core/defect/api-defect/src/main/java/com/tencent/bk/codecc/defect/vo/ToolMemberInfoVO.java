package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 工具成员信息 VO
 *
 * @date 2024/05/17
 */
@Data
@ApiModel("工具成员信息")
public class ToolMemberInfoVO {
    @ApiModelProperty("用户名")
    private String username;
    @ApiModelProperty("角色")
    private UserRoleVO role;
}
