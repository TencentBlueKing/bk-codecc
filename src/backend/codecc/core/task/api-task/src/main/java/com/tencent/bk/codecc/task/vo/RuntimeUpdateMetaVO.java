package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "运行时数据更新")
public class RuntimeUpdateMetaVO extends CommonVO {

    /**
     * 任务ID
     */
    private String taskId;
    /**
     * 构建ID
     */
    private String buildId;
    /**
     * 项目ID
     */
    private String projectId;
    /**
     * 项目名称
     */
    private String projectName;
    /**
     * 流水线对应的插件ID
     */
    private String pipelineTaskId;
    /**
     * 流水线对应的插件名称
     */
    private String pipelineTaskName;

    /**
     * 超时时间（S）
     */
    private Integer timeout;

    /**
     * 超时时间（S）
     */
    private Boolean fileCacheEnable;
}
