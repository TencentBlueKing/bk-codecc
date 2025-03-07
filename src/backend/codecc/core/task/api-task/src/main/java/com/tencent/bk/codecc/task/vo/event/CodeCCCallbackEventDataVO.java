package com.tencent.bk.codecc.task.vo.event;

import lombok.Data;

@Data
public class CodeCCCallbackEventDataVO {
    /**
     * 项目ID
     */
    private String projectId;
    /**
     * 流水线ID
     */
    private String pipelineId;
    /**
     * 任务ID
     */
    private Long taskId;

}
