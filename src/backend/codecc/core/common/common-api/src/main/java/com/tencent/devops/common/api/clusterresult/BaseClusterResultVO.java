package com.tencent.devops.common.api.clusterresult;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@JsonTypeInfo(use = Id.NAME, property = "type", visible = true, include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StandardClusterResultVO.class, name = "STANDARD"),
        @JsonSubTypes.Type(value = DefectClusterResultVO.class, name = "DEFECT"),
        @JsonSubTypes.Type(value = DupcClusterResultVO.class, name = "DUPC"),
        @JsonSubTypes.Type(value = CcnClusterResultVO.class, name = "CCN"),
        @JsonSubTypes.Type(value = SecurityClusterResultVO.class, name = "SECURITY"),
        @JsonSubTypes.Type(value = SCAClusterResultVO.class, name = "SCA")
})
@Schema
public class BaseClusterResultVO {
    @Schema
    private Long taskId;

    @Schema
    private String buildId;

    @Schema
    private String type;

    @Schema
    private Integer toolNum;

    @Schema
    private List<String> toolList;

    @Schema
    private Integer totalCount;

    @Schema
    private Integer newCount;

    @Schema
    private Integer fixCount;

    @Schema
    private Integer maskCount;

    @Schema
    private Long totalLines;

    @Schema
    private Integer ccnBeyondThresholdSum;
}
