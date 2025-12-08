package com.tencent.bk.codecc.defect.vo.sca;

import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.devops.common.api.pojo.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SCA许可证告警清单查询返回视图")
public class SCALicenseDefectQueryRspVO extends CommonDefectQueryRspVO {
    /**
     * 风险系数高的个数
     */
    @Schema(description = "风险系数高的个数")
    private int highCount;

    /**
     * 风险系数中的个数
     */
    @Schema(description = "风险系数中的个数")
    private int mediumCount;

    /**
     * 风险系数高的个数
     */
    @Schema(description = "风险系数低的个数")
    private int lowCount;

    /**
     * 风险系数未知的个数
     */
    @Schema(description = "风险系数未知的个数")
    private int unknownCount;

    /**
     * 许可证总数
     */
    private int totalCount;

    @Schema(description = "许可证告警列表")
    private Page<SCALicenseVO> licenseList;
}
