package com.tencent.bk.codecc.defect.vo.checkerset;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 修改规则集请求体视图
 *
 * @version V4.0
 * @date 2019/11/4
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "规则集视图")
public class UpdateCheckerSetReqVO extends CommonVO
{
    /**
     * 规则集名称
     */
    @Schema(description = "规则集名称")
    private String checkerSetName;

    /**
     * 规则集可见范围1：公开；2：仅我的项目；
     */
    @Schema(description = "规则集可见范围")
    private Integer scope;

    /**
     * 是否升级我的项目中关联的该规则集的其他任务 Y：是   N：否
     */
    @Schema(description = "是否升级我的项目中关联的该规则集的其他任务")
    private String upgradeMyOtherTasks;
}
