package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "用户角色 VO")
public class UserRoleVO {
    @Schema(description = "角色名")
    private String name;
    @Schema(description = "角色 id")
    private Integer id;
}
