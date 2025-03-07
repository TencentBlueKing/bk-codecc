package com.tencent.bk.codecc.defect.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskInvalidToolDefectVO {
    /**
     * 任务ID
     */
    private Long taskId;
    /**
     * 任务来源
     */
    private String createFrom;
    /**
     * 构建ID
     */
    private String buildId;
    /**
     * 失效的工具
     */
    private String invalidTool;

    /**
     * 工具类型
     */
    private String toolType;
}
