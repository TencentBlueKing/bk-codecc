package com.tencent.bk.codecc.task.vo.preci;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("项目IDE信息")
public class ProjectIdeInfoReqVO {

    @ApiModelProperty("本地系统类型")
    private String osType;

    @ApiModelProperty("本地IP")
    private String hostIp;

    @ApiModelProperty("本地机器名称")
    private String hostName;

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("项目路径")
    private String projectPath;

    @ApiModelProperty("IDE类型")
    private String ideType;

    @ApiModelProperty("IDE名称")
    private String ideName;

    @ApiModelProperty("IDEA程序启动路径")
    private String idePath;

    @ApiModelProperty("git路径")
    private String gitUrl;

    @ApiModelProperty("注册表名称")
    private String register;
}