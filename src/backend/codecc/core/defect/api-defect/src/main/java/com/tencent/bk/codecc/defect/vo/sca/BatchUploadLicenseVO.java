package com.tencent.bk.codecc.defect.vo.sca;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;

@Data
@ApiModel("批量上报证书信息")
public class BatchUploadLicenseVO {

    @ApiModelProperty("证书名称")
    private String name;

    @ApiModelProperty("全名")
    private String fullName;

    @ApiModelProperty("描述")
    private String summary;

    @ApiModelProperty("是否是osi认证")
    private Boolean osi;

    @ApiModelProperty("是否是FSF认证")
    private Boolean fsf;

    @ApiModelProperty("是否是SPDX认证")
    private Boolean spdx;

    @ApiModelProperty("许可证链接")
    private List<String> urls;

    @ApiModelProperty("状态")
    private Integer status;
}
