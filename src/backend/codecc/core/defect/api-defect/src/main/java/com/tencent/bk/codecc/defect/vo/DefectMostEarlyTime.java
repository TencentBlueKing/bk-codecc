package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "最早修复时间视图")
public class DefectMostEarlyTime{

    @Schema(description = "修复动作[修复、忽略、屏蔽]")
    private Integer action;

    @Schema(description = "最早修复时间")
    private Long time;

}