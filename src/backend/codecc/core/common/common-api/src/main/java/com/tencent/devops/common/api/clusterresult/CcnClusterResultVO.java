package com.tencent.devops.common.api.clusterresult;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Schema
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CcnClusterResultVO extends BaseClusterResultVO {
    public CcnClusterResultVO(String type, Integer totalCount, List<String> toolList) {
        this.setToolNum(totalCount);
        this.setType(type);
        this.setToolList(toolList);
    }

    @Schema
    private Integer totalCount;

    @Schema
    private Integer totalChange;

    @Schema
    private Double averageThousandDefect;

    @Schema
    private Double averageThousandDefectChange;
}
