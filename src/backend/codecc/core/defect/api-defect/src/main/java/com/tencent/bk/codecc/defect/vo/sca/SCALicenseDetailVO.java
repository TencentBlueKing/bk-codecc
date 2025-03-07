package com.tencent.bk.codecc.defect.vo.sca;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("SCA证书详情类视图")
public class SCALicenseDetailVO extends SCALicenseVO {
    @ApiModelProperty("许可证Id")
    private long licenseId;

    @ApiModelProperty("许可证链接")
    private List<String> urls;

    @ApiModelProperty("GPL兼容性描述")
    private String gplDesc;

    @ApiModelProperty("风险说明")
    private String severityDesc;

    @ApiModelProperty("摘要")
    private String summary;

    @ApiModelProperty("使用许可证的义务，必须")
    private List<String> required;

    @ApiModelProperty("使用许可证的义务，无需")
    private List<String> unnecessary;

    @ApiModelProperty("许可证授予的权利，允许内容")
    private List<String> permitted;

    @ApiModelProperty("许可证授予的权利，禁止内容")
    private List<String> forbidden;
}
