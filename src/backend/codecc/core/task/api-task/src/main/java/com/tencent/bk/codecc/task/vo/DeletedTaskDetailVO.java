package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "删除任务详细信息")
public class DeletedTaskDetailVO extends TaskDetailVO {
    /**
     * 删除人
     */
    @Schema(description = "删除人")
    private String deleteBy;

    /**
     * 删除时间
     */
    @Schema(description = "删除时间")
    private long deleteDate;
}
