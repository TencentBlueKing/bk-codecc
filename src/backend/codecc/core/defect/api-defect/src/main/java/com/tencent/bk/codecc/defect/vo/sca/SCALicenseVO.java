package com.tencent.bk.codecc.defect.vo.sca;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "SCA证书类视图")
public class SCALicenseVO {
    @Schema(description = "告警数据库id")
    private String entityId;

    @Schema(description = "任务Id")
    private long taskId;

    @Schema(description = "工具名")
    private String toolName;

    @Schema(description = "许可证名称")
    private String name;

    /**
     * 告警状态：NEW(1), FIXED(2), IGNORE(4), PATH_MASK(8), CHECKER_MASK(16);
     */
    @Schema(description = "告警状态")
    private int status;

    /**
     * 4个等级：高危、中危、低危、未知
     */
    @Schema(description = "风险等级")
    private int severity;

    @Schema(description = "许可证全名")
    private String fullName;

    @Schema(description = "OSI认证")
    private boolean osi;

    @Schema(description = "FSF许可")
    private boolean fsf;

    @Schema(description = "SPDX认证")
    private boolean spdx;

    @Schema(description = "GPL兼容")
    private Boolean gplCompatible;
}
