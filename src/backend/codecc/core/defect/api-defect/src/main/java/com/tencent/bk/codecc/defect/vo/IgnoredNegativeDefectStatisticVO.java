package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 以误报为理由忽略的告警的统计数据结果视图
 *
 * @date 2024/01/15
 */
@Data
@ApiModel("以误报为理由忽略的告警的统计数据结果视图")
public class IgnoredNegativeDefectStatisticVO {

    @ApiModelProperty("总计误报数")
    private Long total;

    @ApiModelProperty("已确认的误报数")
    private Long confirmed;

    @ApiModelProperty("待确认的误报数")
    private Long unconfirmed;

    @ApiModelProperty("涉及到的代码库数量")
    private Long repos;
}
