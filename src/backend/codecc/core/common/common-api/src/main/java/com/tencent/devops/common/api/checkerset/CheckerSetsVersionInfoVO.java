package com.tencent.devops.common.api.checkerset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "规则集版本视图")
public class CheckerSetsVersionInfoVO {
    @Schema(description = "规则集id")
    private String checkerSetId;

    @Schema(description = "规则集版本")
    private Integer version;
}
