package com.tencent.bk.codecc.defect.vo;

import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 代码统计查询返回视图
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "代码统计查询返回视图")
public class CLOCDefectQueryRspVO extends CommonDefectQueryRspVO {

    @Schema(description = "各项统计指标总和")
    private CLOCDefectQueryRspInfoVO totalInfo;

    @Schema(description = "按语言分类统计信息")
    private List<CLOCDefectQueryRspInfoVO> languageInfo;

    @Schema(description = "占比较小语言统计信息，总行数小于1%")
    private CLOCDefectQueryRspInfoVO otherInfo;

}
