package com.tencent.bk.codecc.defect.vo.sca;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SCA组件详情类视图")
public class SCAPackageDetailVO extends SCAPackageVO {
    @Schema(description = "发布日期")
    private String releaseDate;

    @Schema(description = "组件描述")
    private String description;

    @Schema(description = "下载地址")
    private String downloadLocation;

    @Schema(description = "主页URL")
    private String homepage;

    @Schema(description = "源代码URL")
    private String sourceUrl;

    @Schema(description = "文档URL")
    private String docUrl;

    @Schema(description = "依赖来源")
    private Boolean filesAnalyzed;

    @Schema(description = "依赖层级")
    private int depth;

    @Schema(description = "引入的文件列表")
    private List<SCAPackageFileInfoVO> fileInfos;

    @Schema(description = "供应商")
    private String supplier;

    @Schema(description = "组件漏洞信息")
    private List<SCAVulnerabilityVO> vulnerabilityList;
}
