package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工具成员信息 VO
 *
 * @date 2024/05/17
 */
@Data
@Schema(description = "工具成员信息")
public class ToolMemberInfoVO {
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "角色")
    private UserRoleVO role;
}
