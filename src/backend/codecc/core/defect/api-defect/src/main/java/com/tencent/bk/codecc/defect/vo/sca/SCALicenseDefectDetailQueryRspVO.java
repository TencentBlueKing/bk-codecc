package com.tencent.bk.codecc.defect.vo.sca;

import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SCA证书告警详情查询返回视图")
public class SCALicenseDefectDetailQueryRspVO extends CommonDefectDetailQueryRspVO {

    @Schema(description = "证书详情")
    private SCALicenseDetailVO scaLicenseDetailVO;
}
