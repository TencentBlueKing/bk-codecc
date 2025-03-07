package com.tencent.bk.codecc.task.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskInfoWithSortedToolConfigResponse {

    private List<TaskBase> taskBaseList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaskBase {

        private long taskId;
        private String projectId;
        private String pipelineId;
        private String nameCn;
        private String createFrom;
        private List<ToolConfigInfoVO> toolConfigInfoList;
    }
}
