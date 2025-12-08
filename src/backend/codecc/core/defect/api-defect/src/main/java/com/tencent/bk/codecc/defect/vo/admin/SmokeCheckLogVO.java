package com.tencent.bk.codecc.defect.vo.admin;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 冒烟检查日志视图
 *
 * @version V1.0
 * @date 2021/5/31
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "冒烟检查日志视图")
public class SmokeCheckLogVO extends CommonVO {

    @Schema(description = "工具名")
    private String toolName;

    @Schema(description = "符合条件的任务ID数")
    private Integer taskIdCount;

    @Schema(description = "筛选任务ID的分级条件")
    private String filterText;

    @Schema(description = "备注")
    private String remarks;
}
