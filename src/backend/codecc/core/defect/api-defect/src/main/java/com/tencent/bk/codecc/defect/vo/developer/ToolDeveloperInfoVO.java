package com.tencent.bk.codecc.defect.vo.developer;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "工具开发者信息VO")
public class ToolDeveloperInfoVO {
    @Schema(description = "工具名")
    private String toolName;
    @Schema(description = "开发者集合")
    private Set<String> developers;
    @Schema(description = "拥有者集合")
    private Set<String> owners;
    @Schema(description = "管理员集合")
    private Set<String> masters;
}
