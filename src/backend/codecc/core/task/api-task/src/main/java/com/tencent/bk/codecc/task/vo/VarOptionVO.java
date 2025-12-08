package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "规则参数操作选项")
public class VarOptionVO
{
    @Schema(description = "名称")
    private String name;

    @Schema(description = "ID")
    private String id;
}