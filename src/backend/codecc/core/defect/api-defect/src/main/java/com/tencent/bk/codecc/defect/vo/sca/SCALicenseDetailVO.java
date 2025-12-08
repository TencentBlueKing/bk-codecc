package com.tencent.bk.codecc.defect.vo.sca;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SCA证书详情类视图")
public class SCALicenseDetailVO extends SCALicenseVO {
    @Schema(description = "许可证Id")
    private long licenseId;

    @Schema(description = "许可证链接")
    private List<String> urls;

    @Schema(description = "GPL兼容性描述")
    private String gplDesc;

    @Schema(description = "风险说明")
    private String severityDesc;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "使用许可证的义务，必须")
    private List<String> required;

    @Schema(description = "使用许可证的义务，无需")
    private List<String> unnecessary;

    @Schema(description = "许可证授予的权利，允许内容")
    private List<String> permitted;

    @Schema(description = "许可证授予的权利，禁止内容")
    private List<String> forbidden;
}
