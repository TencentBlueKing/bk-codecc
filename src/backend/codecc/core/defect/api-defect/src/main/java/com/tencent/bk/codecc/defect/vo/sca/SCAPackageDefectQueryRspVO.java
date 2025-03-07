package com.tencent.bk.codecc.defect.vo.sca;

import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.devops.common.api.pojo.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("SCA组件告警清单查询返回视图")
public class SCAPackageDefectQueryRspVO extends CommonDefectQueryRspVO {
    /**
     * 风险系数高的个数
     */
    @ApiModelProperty("风险系数高的个数")
    private int highCount;

    /**
     * 风险系数中的个数
     */
    @ApiModelProperty("风险系数中的个数")
    private int mediumCount;

    /**
     * 风险系数高的个数
     */
    @ApiModelProperty("风险系数低的个数")
    private int lowCount;

    /**
     * 风险系数未知的个数
     */
    @ApiModelProperty("风险系数未知的个数")
    private int unknownCount;

    /**
     * 组件总数
     */
    private int totalCount;

    @ApiModelProperty("待修复告警数")
    private long existCount;

    @ApiModelProperty("已修复告警数")
    private long fixCount;

    @ApiModelProperty("已忽略告警数")
    private long ignoreCount;

    @ApiModelProperty("已屏蔽告警数")
    private long maskCount;

    @ApiModelProperty("组件告警列表")
    private Page<SCAPackageVO> packageList;
}
