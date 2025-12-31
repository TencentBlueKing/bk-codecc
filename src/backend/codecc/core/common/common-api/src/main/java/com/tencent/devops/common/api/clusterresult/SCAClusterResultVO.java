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
public class SCAClusterResultVO extends BaseClusterResultVO {
    public SCAClusterResultVO(String type, Integer totalCount, List<String> toolList) {
        this.setToolNum(totalCount);
        this.setType(type);
        this.setToolList(toolList);
    }

    @Schema(description = "组件数量")
    private Long packageCount;

    @Schema(description = "漏洞数量")
    private Long newVulCount;

    @Schema(description = "高风险漏洞数量")
    private Long newHighVulCount;

    @Schema(description = "中风险漏洞数量")
    private Long newMediumVulCount;

    @Schema(description = "低风险漏洞数量")
    private Long newLowVulCount;

    @Schema(description = "证书数量")
    private Long licenseCount;
}
