package com.tencent.bk.codecc.defect.vo.sca;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("SCA证书类视图")
public class SCALicenseVO {
    @ApiModelProperty("告警数据库id")
    private String entityId;

    @ApiModelProperty("任务Id")
    private long taskId;

    @ApiModelProperty("工具名")
    private String toolName;

    @ApiModelProperty("许可证名称")
    private String name;

    /**
     * 告警状态：NEW(1), FIXED(2), IGNORE(4), PATH_MASK(8), CHECKER_MASK(16);
     */
    @ApiModelProperty("告警状态")
    private int status;

    /**
     * 4个等级：高危、中危、低危、未知
     */
    @ApiModelProperty("风险等级")
    private int severity;

    @ApiModelProperty("许可证全名")
    private String fullName;

    @ApiModelProperty("OSI认证")
    private boolean osi;

    @ApiModelProperty("FSF许可")
    private boolean fsf;

    @ApiModelProperty("SPDX认证")
    private boolean spdx;

    @ApiModelProperty("GPL兼容")
    private Boolean gplCompatible;
}
