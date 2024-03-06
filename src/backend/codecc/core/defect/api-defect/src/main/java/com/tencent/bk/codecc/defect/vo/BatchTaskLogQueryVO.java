package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("批量查询任务的Tasklog")
public class BatchTaskLogQueryVO {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务对应的工具列表
     */
    private Set<String> toolSet;

    /**
     * 构建iD
     */
    private String buildId;
}
