package com.tencent.devops.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公共分页视图对象
 *
 * @version V1.0
 * @date 2019/11/22
 */

@Data
public class CommonPageVO
{
    @Schema(description = "第几页")
    private Integer pageNum;

    @Schema(description = "每页多少条")
    private Integer pageSize;

    @Schema(description = "排序字段")
    private String sortField;

    @Schema(description = "排序类型")
    private String sortType;

}
