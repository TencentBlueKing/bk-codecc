package com.tencent.bk.codecc.defect.vo.admin;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 冒烟检查日志视图
 *
 * @version V1.0
 * @date 2021/5/31
 */

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("冒烟检查日志视图")
public class SmokeCheckLogVO extends CommonVO {

    @ApiModelProperty("工具名")
    private String toolName;

    @ApiModelProperty("符合条件的任务ID数")
    private Integer taskIdCount;

    @ApiModelProperty("筛选任务ID的分级条件")
    private String filterText;

    @ApiModelProperty("备注")
    private String remarks;
}
