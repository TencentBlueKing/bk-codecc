package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@ApiModel("工具接入停用返回类")
public class ToolTaskInfoVO {
    @ApiModelProperty(value = "任务id列表", required = true)
    private Map<Integer,List<TaskDetailVO>> taskIdMap;

    @ApiModelProperty(value = "项目")
    private int totalProjectCount;

    @ApiModelProperty
    private int totalTaskCount;
}
