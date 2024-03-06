package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("删除任务详细信息")
public class DeletedTaskDetailVO extends TaskDetailVO {
    /**
     * 删除人
     */
    @ApiModelProperty(value = "删除人")
    private String deleteBy;

    /**
     * 删除时间
     */
    @ApiModelProperty("删除时间")
    private long deleteDate;
}
