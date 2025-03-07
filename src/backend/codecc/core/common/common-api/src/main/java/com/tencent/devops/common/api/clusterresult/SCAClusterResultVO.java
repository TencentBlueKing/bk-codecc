package com.tencent.devops.common.api.clusterresult;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@ApiModel
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SCAClusterResultVO extends BaseClusterResultVO {
    public SCAClusterResultVO(String type, Integer totalCount, List<String> toolList) {
        this.setToolNum(totalCount);
        this.setType(type);
        this.setToolList(toolList);
    }

    @ApiModelProperty("组件数量")
    private Long packageCount;

    @ApiModelProperty("漏洞数量")
    private Long newVulCount;

    @ApiModelProperty("高风险漏洞数量")
    private Long newHighVulCount;

    @ApiModelProperty("中风险漏洞数量")
    private Long newMediumVulCount;

    @ApiModelProperty("低风险漏洞数量")
    private Long newLowVulCount;

    @ApiModelProperty("证书数量")
    private Long licenseCount;
}
