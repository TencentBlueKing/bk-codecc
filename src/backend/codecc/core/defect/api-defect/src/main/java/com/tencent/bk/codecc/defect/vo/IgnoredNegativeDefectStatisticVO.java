package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 以误报为理由忽略的告警的统计数据结果视图
 *
 * @date 2024/01/15
 */
@Data
@Schema(description = "以误报为理由忽略的告警的统计数据结果视图")
public class IgnoredNegativeDefectStatisticVO {

    @Schema(description = "总计误报数")
    private Long total;

    @Schema(description = "已确认的误报数")
    private Long confirmed;

    @Schema(description = "待确认的误报数")
    private Long unconfirmed;

    @Schema(description = "涉及到的代码库数量")
    private Long repos;
}
