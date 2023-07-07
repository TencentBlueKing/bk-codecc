package com.tencent.bk.codecc.task.vo.pipeline;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流水线任务数量限制
 *
 * @version V1.0
 * @date 2022/1/11
 */

@ApiModel("流水线任务数量限制")
@Data
@EqualsAndHashCode(callSuper = true)
public class PipelineTaskLimitVO extends CommonVO {

    @ApiModelProperty("任务限制数")
    private String pipelineTaskLimit;

    @ApiModelProperty("暂未使用")
    private Integer status;

    @ApiModelProperty("作用与目标对象")
    private String target;

}
