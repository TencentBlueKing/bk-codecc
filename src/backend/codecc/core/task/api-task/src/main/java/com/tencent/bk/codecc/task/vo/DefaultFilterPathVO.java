package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 默认屏蔽路径列表视图
 *
 * @version V1.0
 * @date 2021/9/27
 */

@Data
@Schema(description = "默认屏蔽路径列表视图")
public class DefaultFilterPathVO {

    @Schema(description = "屏蔽路径")
    private String filterPath;

    @Schema(description = "创建者")
    private String createdBy;

    @Schema(description = "创建时间")
    private Long createDate;
}
