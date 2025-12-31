package com.tencent.devops.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Data;

@Data
@Schema
public class OpenSourceCheckerSetVO {
    @Schema
    private String checkerSetId;

    @Schema
    private Set<String> toolList;

    @Schema
    private String checkerSetType;

    @Schema
    private Integer version;
}
