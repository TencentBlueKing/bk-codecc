package com.tencent.bk.codecc.defect.vo.sca;

import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SCA组件告警详情查询返回视图")
public class SCAPackageDefectDetailQueryRspVO extends CommonDefectDetailQueryRspVO {

    @Schema(description = "SCA组件详情")
    private SCAPackageDetailVO scaPackageDetailVO;
}