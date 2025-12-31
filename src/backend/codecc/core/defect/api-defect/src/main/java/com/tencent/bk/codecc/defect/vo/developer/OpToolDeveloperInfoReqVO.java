package com.tencent.bk.codecc.defect.vo.developer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "OP工具开发者详情视图")
public class OpToolDeveloperInfoReqVO {

    @Schema(description = "工具名")
    @NotBlank(message = "工具名称不能为空")
    private String toolName;

    @Schema(description = "成员ID")
    @NotBlank(message = "成员ID不能为空")
    private String userId;

    @Schema(description = "角色ID")
    @NotNull(message = "角色ID不能为空")
    private Integer roleId;
}
