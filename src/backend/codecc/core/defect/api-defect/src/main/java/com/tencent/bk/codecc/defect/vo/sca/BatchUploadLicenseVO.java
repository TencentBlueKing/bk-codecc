package com.tencent.bk.codecc.defect.vo.sca;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "批量上报证书信息")
public class BatchUploadLicenseVO {

    @Schema(description = "证书名称")
    private String name;

    @Schema(description = "全名")
    private String fullName;

    @Schema(description = "描述")
    private String summary;

    @Schema(description = "是否是osi认证")
    private Boolean osi;

    @Schema(description = "是否是FSF认证")
    private Boolean fsf;

    @Schema(description = "是否是SPDX认证")
    private Boolean spdx;

    @Schema(description = "许可证链接")
    private List<String> urls;

    @Schema(description = "状态")
    private Integer status;
}
