package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "规则参数操作选项")
public class CreateTaskConfigVO {

    @Schema(description = "语言集合")
    List<String> langs;
}
