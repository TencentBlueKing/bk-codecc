package com.tencent.bk.codecc.defect.vo.sca;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SCAVulAffectedPackageVersionVO {
    @Schema(description = "版本")
    private String version;

    @Schema(description = "命中的版本范围")
    private String range;
}
