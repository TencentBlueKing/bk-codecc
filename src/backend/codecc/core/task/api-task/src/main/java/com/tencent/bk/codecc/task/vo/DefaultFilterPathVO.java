package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 默认屏蔽路径列表视图
 *
 * @version V1.0
 * @date 2021/9/27
 */

@Data
@ApiModel("默认屏蔽路径列表视图")
public class DefaultFilterPathVO {

    @ApiModelProperty("屏蔽路径")
    private String filterPath;

    @ApiModelProperty("创建者")
    private String createdBy;

    @ApiModelProperty("创建时间")
    private Long createDate;
}
