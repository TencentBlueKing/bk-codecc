package com.tencent.bk.codecc.defect.vo.sca;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("SCA组件详情类视图")
public class SCAPackageDetailVO extends SCAPackageVO {
    @ApiModelProperty("发布日期")
    private String releaseDate;

    @ApiModelProperty("组件描述")
    private String description;

    @ApiModelProperty("下载地址")
    private String downloadLocation;

    @ApiModelProperty("主页URL")
    private String homepage;

    @ApiModelProperty("源代码URL")
    private String sourceUrl;

    @ApiModelProperty("文档URL")
    private String docUrl;

    @ApiModelProperty("依赖来源")
    private Boolean filesAnalyzed;

    @ApiModelProperty("依赖层级")
    private int depth;

    @ApiModelProperty("引入的文件列表")
    private List<SCAPackageFileInfoVO> fileInfos;

    @ApiModelProperty("供应商")
    private String supplier;

    @ApiModelProperty("组件漏洞信息")
    private List<SCAVulnerabilityVO> vulnerabilityList;
}
