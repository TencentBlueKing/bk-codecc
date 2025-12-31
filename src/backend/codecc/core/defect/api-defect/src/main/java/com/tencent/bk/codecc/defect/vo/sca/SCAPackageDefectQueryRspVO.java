package com.tencent.bk.codecc.defect.vo.sca;

import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.devops.common.api.pojo.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SCA组件告警清单查询返回视图")
public class SCAPackageDefectQueryRspVO extends CommonDefectQueryRspVO {
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
     * 组件总数
     */
    private int totalCount;

    @Schema(description = "待修复告警数")
    private long existCount;

    @Schema(description = "已修复告警数")
    private long fixCount;

    @Schema(description = "已忽略告警数")
    private long ignoreCount;

    @Schema(description = "已屏蔽告警数")
    private long maskCount;

    @Schema(description = "组件告警列表")
    private Page<SCAPackageVO> packageList;
}
