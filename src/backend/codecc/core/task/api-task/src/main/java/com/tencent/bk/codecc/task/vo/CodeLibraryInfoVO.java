package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务代码库配置
 *
 * @version V1.0
 * @date 2020/11/27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "任务代码库配置")
public class CodeLibraryInfoVO {
    @Schema(description = "链接地址")
    private String url;

    @Schema(description = "别名")
    private String aliasName;

    @Schema(description = "分支")
    private String branch;
}
