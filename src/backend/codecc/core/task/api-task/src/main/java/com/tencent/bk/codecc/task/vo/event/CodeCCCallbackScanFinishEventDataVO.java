package com.tencent.bk.codecc.task.vo.event;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 扫描结束CallBack信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class CodeCCCallbackScanFinishEventDataVO extends CodeCCCallbackEventDataVO {

    private String userId;
    private String buildId;
    private List<String> tools;
    private List<String> lang;
    private Long langValue;
    private Long startTime;
    private Long endTime;
    private Integer status;
    private String createFrom;

    public CodeCCCallbackScanFinishEventDataVO(
            String projectId,
            String pipelineId,
            Long taskId,
            String userId,
            String buildId,
            List<String> tools,
            List<String> lang,
            Long langValue,
            Long startTime,
            Long endTime,
            Integer status,
            String createFrom
    ) {
        this(userId, buildId, tools, lang, langValue, startTime, endTime, status, createFrom);
        setProjectId(projectId);
        setPipelineId(pipelineId);
        setTaskId(taskId);
    }
}
