package com.tencent.devops.common.api.checkerset;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("规则集版本视图")
public class CheckerSetsVersionInfoVO {
    @ApiModelProperty("规则集id")
    private String checkerSetId;

    @ApiModelProperty("规则集版本")
    private Integer version;
}
