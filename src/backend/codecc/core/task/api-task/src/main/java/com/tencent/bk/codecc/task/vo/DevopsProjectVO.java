package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 持续集成项目视图
 *
 * @version V1.0
 * @date 2019/5/22
 */
@Data
@AllArgsConstructor
@Schema(description = "持续集成项目视图")
public class DevopsProjectVO {
    @Schema(description = "项目id")
    private String projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目代码")
    private String projectCode;

    @Schema(description = "项目类型")
    private Integer projectType;

}
