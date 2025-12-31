package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 规则子选项
 *
 * @version V4.0
 * @date 2019/10/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "规则子选项视图实体类")
public class CovSubcategoryVO extends CommonVO
{
    @Schema(description = "规则子选项唯一标识")
    private String checkerSubcategoryKey;

    @Schema(description = "规则资源向名称")
    private String checkerSubcategoryName;

    @Schema(description = "规则子选项详情")
    private String checkerSubcategoryDetail;

    @Schema(description = "规则名唯一标识")
    private String checkerKey;

    @Schema(description = "工具可识别规则名")
    private String checkerName;

    @Schema(description = "语言")
    private int language;
}
