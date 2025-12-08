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
public class DefectClusterResultVO extends BaseClusterResultVO {
    public DefectClusterResultVO(String type, Integer totalCount, List<String> toolList) {
        this.setToolNum(totalCount);
        this.setType(type);
        this.setToolList(toolList);
    }

    @Schema
    private Integer totalCount;

    @Schema
    private Integer newDefectCount;

    @Schema
    private Integer fixDefectCount;

    @Schema
    private Integer maskDefectCount;
}
