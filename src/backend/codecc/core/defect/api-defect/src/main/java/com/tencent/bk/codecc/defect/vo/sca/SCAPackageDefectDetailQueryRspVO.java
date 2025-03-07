package com.tencent.bk.codecc.defect.vo.sca;

import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("SCA组件告警详情查询返回视图")
public class SCAPackageDefectDetailQueryRspVO extends CommonDefectDetailQueryRspVO {

    @ApiModelProperty("SCA组件详情")
    private SCAPackageDetailVO scaPackageDetailVO;
}