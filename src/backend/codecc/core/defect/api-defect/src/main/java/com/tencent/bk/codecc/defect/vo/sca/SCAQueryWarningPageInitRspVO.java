package com.tencent.bk.codecc.defect.vo.sca;

import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码成分页面初始化统计视图
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "代码成分页面初始化统计视图")
public class SCAQueryWarningPageInitRspVO extends QueryWarningPageInitRspVO {
    /**
     * 组件、许可证包含 "未知" 风险等级
     */
    @Schema(description = "风险等级未知的告警数")
    private int unknownCount;
}
