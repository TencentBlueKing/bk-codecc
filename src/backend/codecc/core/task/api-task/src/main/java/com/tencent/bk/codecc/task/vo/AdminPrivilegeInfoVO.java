package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 新版-管理员授权信息
 *
 * @version V1.0
 * @date 2025/4/18
 */

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminPrivilegeInfoVO extends CommonVO {

    @Schema(description = "用户名ID")
    @NotNull(message = "用户ID不能为空")
    private String userId;

    @Schema(description = "管理员类型")
    @NotNull(message = "管理员类型不能为空")
    private String privilegeType;

    @Schema(description = "已授权的BG id")
    private List<Integer> bgIdList;

    @Schema(description = "已授权的BG")
    private List<String> bgNameList;

    @Schema(description = "已授权的来源平台：开源/非开源")
    private List<String> createFroms;

    @Schema(description = "申请账户有效天数")
    private Integer validityDays;

    @Schema(description = "账号启用状态")
    private Boolean status;

    @Schema(description = "备注/权限授予原因")
    @NotNull(message = "申请授权原因不能为空")
    private String reason;
}
