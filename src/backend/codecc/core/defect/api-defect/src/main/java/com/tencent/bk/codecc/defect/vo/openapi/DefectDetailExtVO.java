package com.tencent.bk.codecc.defect.vo.openapi;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Cov告警列表扩展视图(添翼)
 *
 * @version V1.0
 * @date 2019/12/6
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Cov告警列表扩展视图")
public class DefectDetailExtVO extends DefectDetailVO
{
    @Schema(description = "告警唯一标识")
    private String id;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "规则类型")
    private String displayCategory;

    @Schema(description = "规则子类")
    private String displayType;

    @Schema(description = "告警创建时间")
    private long createTime;

    @Schema(description = "告警修复时间")
    private long fixedTime;

    @Schema(description = "告警忽略时间")
    private long ignoreTime;

    @Schema(description = "告警屏蔽时间")
    private long excludeTime;


}
