package com.tencent.bk.codecc.defect.vo.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据报表作者信息视图
 *
 * @version V1.0
 * @date 2019/12/4
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据报表作者信息视图")
public class ChartAuthorBaseVO {
    @Schema(description = "作者名称")
    String authorName;

    @Schema(description = "总数")
    Integer total;
}
