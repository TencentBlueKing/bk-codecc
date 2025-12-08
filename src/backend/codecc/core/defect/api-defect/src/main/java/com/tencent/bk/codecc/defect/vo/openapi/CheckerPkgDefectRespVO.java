package com.tencent.bk.codecc.defect.vo.openapi;

import com.tencent.devops.common.api.pojo.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 工具规则包告警统计响应视图
 *
 * @date 2019/11/13
 * @version V1.0
 */

@Data
@Schema(description = "工具规则包告警统计响应视图")
public class CheckerPkgDefectRespVO
{
    @Schema(description = "按规则包维度统计告警数")
    private Map<String, PkgDefectDetailVO> statisticsChecker;

    @Schema(description = "按任务维度统计告警数")
    private Page<TaskDefectVO> statisticsTask;

    @Schema(description = "规则数")
    private Integer checkerCount;

    @Schema(description = "任务数")
    private Integer taskCount;

}
