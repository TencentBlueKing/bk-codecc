package com.tencent.bk.codecc.task.vo.pipeline;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流水线任务数量限制
 *
 * @version V1.0
 * @date 2022/1/11
 */

@Schema(description = "流水线任务数量限制")
@Data
@EqualsAndHashCode(callSuper = true)
public class PipelineTaskLimitVO extends CommonVO {

    @Schema(description = "任务限制数")
    private String pipelineTaskLimit;

    @Schema(description = "暂未使用")
    private Integer status;

    @Schema(description = "作用与目标对象")
    private String target;

}
