package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 持续集成项目视图
 *
 * @version V1.0
 * @date 2019/5/22
 */
@Data
@AllArgsConstructor
@ApiModel("持续集成项目视图")
public class DevopsProjectVO {
    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("项目代码")
    private String projectCode;

    @ApiModelProperty("项目类型")
    private Integer projectType;

}
