package com.tencent.bk.codecc.defect.vo.sca;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SCAVulAffectPackageVO {

    @Schema(description = "包名")
    private String packageName;

    @Schema(description = "版本")
    private List<SCAVulAffectedPackageVersionVO> versions;

    @Schema(description = "修复建议")
    private String fixAdvice;
}
