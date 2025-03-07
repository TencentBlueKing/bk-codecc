package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("GitHub同步配置")
@Data
@EqualsAndHashCode(callSuper = true)
public class GithubSyncVO extends CommonVO {

    @ApiModelProperty("value")
    private String value;

    @ApiModelProperty("paramType")
    private String paramType;

}
