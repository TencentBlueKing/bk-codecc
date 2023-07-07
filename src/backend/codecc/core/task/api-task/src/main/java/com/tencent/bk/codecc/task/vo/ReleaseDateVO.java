package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 发布日期视图
 *
 * @version V1.0
 * @date 2021/8/18
 */

@Data
@ApiModel("发布日期视图")
public class ReleaseDateVO {

    @ApiModelProperty("管理类型")
    private String manageType;

    @ApiModelProperty("版本类型")
    private String versionType;

    @ApiModelProperty("正式版日期")
    private Long prodDate;

    @ApiModelProperty("预发布版日期")
    private Long preProdDate;

}
