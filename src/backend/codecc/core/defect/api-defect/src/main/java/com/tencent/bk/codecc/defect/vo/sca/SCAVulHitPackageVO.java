package com.tencent.bk.codecc.defect.vo.sca;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SCAVulHitPackageVO {

    @Schema(description = "包名")
    private String packageName;

    @Schema(description = "版本")
    private String version;
}
