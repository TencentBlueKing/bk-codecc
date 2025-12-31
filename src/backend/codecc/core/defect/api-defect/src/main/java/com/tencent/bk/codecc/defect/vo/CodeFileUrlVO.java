package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * 告警代码文件仓库url
 * 
 * @date 2019/11/29
 * @version V2.0
 */
@Data
public class CodeFileUrlVO
{
    @NotEmpty(message = "流名称不能为空")
    @Schema(description = "流名称", required = true)
    private String streamName;

    @Schema(description = "工具名称", required = true)
    private String toolName;

    @Schema(description = "所有文件的代码仓库url", required = true)
    private String fileList;
}
