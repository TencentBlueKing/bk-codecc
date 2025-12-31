package com.tencent.bk.codecc.task.vo;


import java.util.List;
import lombok.Data;

@Data
public class CodeCCCallbackRegisterVO {

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

    /**
     * 事件列表，目前仅有扫描结束事件
     */
    private List<String> events;

    /**
     * 回调的URL
     */
    private String callbackUrl;

    /**
     * 是否启用
     */
    private Boolean enable;

}
