package com.tencent.bk.codecc.defect.vo.checkerset;

import lombok.Data;

/**
 * 使用规则集的任务详情
 */
@Data
public class TaskUsageDetailVO {

    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 任务中文名称
     */
    private String nameCn;

    /**
     * 流水线id
     */
    private String pipelineId;

    /**
     * 任务状态
     */
    private Integer status;

}
